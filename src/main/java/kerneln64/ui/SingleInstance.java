package kerneln64.ui;

import kerneln64.Main;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.*;
import java.util.UUID;
import java.util.concurrent.*;

/** Per-user OS lock plus authenticated loopback handoff, shared by EXE and Java launches. */
public final class SingleInstance {
    private static FileChannel channel;
    private static FileLock lock;
    private static ServerSocket server;
    private static String token;
    private static boolean ready;
    private static final java.util.ArrayDeque<LaunchArguments> pending=new java.util.ArrayDeque<>();
    private static final ExecutorService launches=Executors.newSingleThreadExecutor(action->{Thread t=new Thread(action,"KernelN64 launch requests");t.setDaemon(true);return t;});
    private SingleInstance(){}
    private static Path endpoint(){
        String user=System.getProperty("user.name","")+"|"+System.getProperty("user.home","");
        return Path.of(System.getProperty("java.io.tmpdir"),"KernelN64-"+UUID.nameUUIDFromBytes(user.getBytes(java.nio.charset.StandardCharsets.UTF_8))+".instance");
    }
    /** Returns false after notifying the owner, before creating hardware, logs or a window. */
    public static synchronized boolean start(LaunchArguments launch)throws IOException{
        if(lock!=null){enqueue(launch);return false;}
        Path file=endpoint();FileChannel candidate=FileChannel.open(file,StandardOpenOption.CREATE,StandardOpenOption.READ,StandardOpenOption.WRITE);
        FileLock claimed;
        // Windows enforces byte-range locks on reads too. Keep endpoint data outside byte zero.
        try{claimed=candidate.tryLock(0,1,false);}catch(OverlappingFileLockException e){claimed=null;}
        if(claimed==null){
            candidate.close();
            try{forward(file,launch);}
            catch(IOException unavailable){
                if(launch.rom()!=null)throw unavailable;
                // A first-launch double click needs no file handoff. The owner is already opening.
                System.out.println("KernelN64 is already open; using the existing window");
            }
            return false;
        }
        try{
            int previousPort=0;String previousToken=null;
            try{
                ByteBuffer previous=ByteBuffer.allocate(128);int count=candidate.read(previous,1);
                if(count>=6){DataInputStream in=new DataInputStream(new ByteArrayInputStream(previous.array(),0,count));int port=in.readInt();String key=in.readUTF();
                    if(port>0&&port<=65535&&UUID.fromString(key).toString().equals(key)){previousPort=port;previousToken=key;}}
            }catch(IOException|IllegalArgumentException ignored){}
            ServerSocket listener=new ServerSocket();
            try{listener.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"),previousPort),8);}
            catch(BindException busy){listener.close();listener=new ServerSocket();listener.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"),0),8);}
            server=listener;token=previousToken==null?UUID.randomUUID().toString():previousToken;channel=candidate;lock=claimed;
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(DataOutputStream out=new DataOutputStream(bytes)){out.writeInt(listener.getLocalPort());out.writeUTF(token);}
            // Reuse the endpoint across restarts, avoiding stale addresses in simultaneous launches.
            if(previousToken==null||previousPort!=listener.getLocalPort()){
                candidate.truncate(1);candidate.position(1);ByteBuffer data=ByteBuffer.wrap(bytes.toByteArray());while(data.hasRemaining())candidate.write(data);candidate.force(true);
            }
            if(launch.rom()!=null)pending.add(launch);
            Runtime.getRuntime().addShutdownHook(new Thread(SingleInstance::close,"KernelN64 instance cleanup"));
            Thread thread=new Thread(SingleInstance::listen,"KernelN64 instance listener");thread.setDaemon(true);thread.start();return true;
        }catch(IOException|RuntimeException e){claimed.release();candidate.close();if(server!=null)server.close();server=null;channel=null;lock=null;throw e;}
    }
    private static void forward(Path file,LaunchArguments launch)throws IOException{
        IOException last=null;
        // An owner can hold the lock briefly before publishing its listening endpoint.
        for(int attempt=0;attempt<20;attempt++){
            try(FileChannel endpoint=FileChannel.open(file,StandardOpenOption.READ,StandardOpenOption.WRITE)){
                // Read the live shared file through a channel, outside the mutex byte.
                ByteBuffer bytes=ByteBuffer.allocate(128);int size=endpoint.read(bytes,1);
                if(size<6)throw new EOFException("instance endpoint is not ready");
                DataInputStream metadata=new DataInputStream(new ByteArrayInputStream(bytes.array(),0,size));
                int port=metadata.readInt();String key=metadata.readUTF();
                try(Socket socket=new Socket()){
                    socket.connect(new InetSocketAddress(InetAddress.getByName("127.0.0.1"),port),250);socket.setSoTimeout(1500);
                    DataOutputStream out=new DataOutputStream(socket.getOutputStream());out.writeUTF(key);out.writeUTF(launch.rom()==null?"":launch.rom().getAbsolutePath());out.writeUTF(launch.state()==null?"":launch.state().getAbsolutePath());out.flush();
                    if(!new DataInputStream(socket.getInputStream()).readBoolean())throw new IOException("existing KernelN64 window rejected the launch request");
                    System.out.println("using the existing KernelN64 window");return;
                }
            }catch(IOException e){last=e;}
            try{Thread.sleep(100);}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IOException("interrupted while contacting existing KernelN64 window",e);}
        }
        throw new IOException("KernelN64 is already open; the existing window could not be contacted",last);
    }
    private static void listen(){
        while(server!=null&&!server.isClosed()){
            try(Socket socket=server.accept()){
                socket.setSoTimeout(1500);DataInputStream in=new DataInputStream(socket.getInputStream());
                if(!token.equals(in.readUTF()))continue;
                String rom=in.readUTF(),state=in.readUTF();
                LaunchArguments launch=LaunchArguments.parse(rom.isEmpty()?new String[0]:state.isEmpty()?new String[]{"--rom",rom}:new String[]{"--rom",rom,"--state",state});
                boolean accepted=enqueue(launch);DataOutputStream out=new DataOutputStream(socket.getOutputStream());out.writeBoolean(accepted);out.flush();
            }catch(IOException|IllegalArgumentException error){if(server!=null&&!server.isClosed())System.err.println("instance handoff: "+error.getMessage());}
        }
    }
    private static synchronized boolean enqueue(LaunchArguments launch){
        if(pending.size()>=16)return false;pending.add(launch);if(ready)drain();return true;
    }
    public static synchronized void ready(){ready=true;drain();}
    private static void drain(){
        while(!pending.isEmpty()){
            LaunchArguments launch=pending.remove();
            MainWindowVisibility.restore();
            if(launch.rom()!=null)launches.execute(()->{
                try{Main.loadRom(launch.rom());if(launch.state()!=null)Main.cpu.submitCommand(()->{try{kerneln64.state.SaveStates.load(launch.state().toPath());}catch(IOException e){throw new UncheckedIOException(e);}}).get();}
                catch(Exception error){HostMessages.error("open rom",error);}
            });
        }
    }
    private static synchronized void close(){
        try{if(server!=null)server.close();}catch(IOException ignored){}
        try{if(lock!=null)lock.release();}catch(IOException ignored){}
        try{if(channel!=null)channel.close();}catch(IOException ignored){}
    }
}
