#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <d3d11.h>
#include <d3dcompiler.h>
#include <wrl/client.h>
#include <jni.h>
#include <jawt.h>
#include <jawt_md.h>
#include <vector>
#include <map>
#include <string>
#include <stdexcept>
#include <cstring>
using Microsoft::WRL::ComPtr;

static void check(HRESULT result, const char* operation) {
    if (FAILED(result)) { char message[256]; sprintf_s(message, "%s failed (HRESULT %08X)", operation, (unsigned)result); throw std::runtime_error(message); }
}
#include "rdp_shader.h"

struct Texture { ComPtr<ID3D11Texture2D> data; ComPtr<ID3D11ShaderResourceView> view; };
struct Surface { ComPtr<ID3D11Texture2D> color, multisample; ComPtr<ID3D11RenderTargetView> target; };
struct Renderer {
 ComPtr<ID3D11Device> device; ComPtr<ID3D11DeviceContext> context; ComPtr<IDXGISwapChain> swap;
 ComPtr<ID3D11Texture2D> color, multisample, depth, staging, feedback; ComPtr<ID3D11ShaderResourceView> feedbackView; ComPtr<ID3D11RenderTargetView> target; ComPtr<ID3D11DepthStencilView> depthView;
 ComPtr<ID3D11VertexShader> vertexShader; ComPtr<ID3D11PixelShader> pixelShader; ComPtr<ID3D11InputLayout> layout;
 ComPtr<ID3D11Buffer> vertices, constants; size_t vertexBytes=0;
 std::map<int,Texture> textures;
 std::map<unsigned long long,ComPtr<ID3D11SamplerState>> samplers;
 std::map<unsigned long long,ComPtr<ID3D11BlendState>> blends;
 std::map<unsigned long long,ComPtr<ID3D11DepthStencilState>> depths;
 std::map<unsigned long long,ComPtr<ID3D11RasterizerState>> rasters;
 int width=0,height=0,samples=1,currentTarget=0; bool software=false;
 std::map<int,Surface> surfaces;
 int depthKey=0;
 std::map<int,std::pair<ComPtr<ID3D11Texture2D>,ComPtr<ID3D11DepthStencilView>>> depthImages;
 void selectDepth(int key) {
  if(key==depthKey)return;
  context->OMSetRenderTargets(0,nullptr,nullptr);
  depthImages[depthKey]={depth,depthView};depthKey=key;
  while(depthImages.size()>8)depthImages.erase(depthImages.begin());
  auto found=depthImages.find(key);
  if(found!=depthImages.end()){depth=found->second.first;depthView=found->second.second;return;}
  D3D11_TEXTURE2D_DESC desc={};desc.Width=width;desc.Height=height;desc.MipLevels=1;desc.ArraySize=1;desc.Format=DXGI_FORMAT_D24_UNORM_S8_UINT;desc.SampleDesc.Count=samples;desc.BindFlags=D3D11_BIND_DEPTH_STENCIL;
  depth.Reset();depthView.Reset();check(device->CreateTexture2D(&desc,nullptr,&depth),"Create depth image");check(device->CreateDepthStencilView(depth.Get(),nullptr,&depthView),"Create depth image view");
  context->ClearDepthStencilView(depthView.Get(),D3D11_CLEAR_DEPTH,1,0);
 }
 void resolve() { if(samples>1)context->ResolveSubresource(color.Get(),0,multisample.Get(),0,DXGI_FORMAT_R8G8B8A8_UNORM);else context->CopyResource(color.Get(),multisample.Get()); }
 void makeSurface() {
  D3D11_TEXTURE2D_DESC desc={}; desc.Width=width;desc.Height=height;desc.MipLevels=1;desc.ArraySize=1;
  desc.Format=DXGI_FORMAT_R8G8B8A8_UNORM;desc.SampleDesc.Count=1;desc.BindFlags=0;
  check(device->CreateTexture2D(&desc,nullptr,&color),"Create resolved target");
  desc.SampleDesc.Count=samples;desc.BindFlags=D3D11_BIND_RENDER_TARGET;
  check(device->CreateTexture2D(&desc,nullptr,&multisample),"Create multisample target");check(device->CreateRenderTargetView(multisample.Get(),nullptr,&target),"Create RTV");
  if(!depth) {
   desc.Format=DXGI_FORMAT_D24_UNORM_S8_UINT;desc.BindFlags=D3D11_BIND_DEPTH_STENCIL;
   check(device->CreateTexture2D(&desc,nullptr,&depth),"Create depth target");check(device->CreateDepthStencilView(depth.Get(),nullptr,&depthView),"Create DSV");
   context->ClearDepthStencilView(depthView.Get(),D3D11_CLEAR_DEPTH|D3D11_CLEAR_STENCIL,1,0);
  }
  float black[4]={0,0,0,1};context->ClearRenderTargetView(target.Get(),black);
 }
 void select(int key) {
  if(key==currentTarget)return;
  context->OMSetRenderTargets(0,nullptr,nullptr);
  depthImages[depthKey]={depth,depthView};
  surfaces[currentTarget]={color,multisample,target};
  auto found=surfaces.find(key);
  if(found==surfaces.end()) { color.Reset();multisample.Reset();target.Reset();makeSurface(); }
  else { auto& s=found->second;color=s.color;multisample=s.multisample;target=s.target; }
  currentTarget=key;
  // Depth images are addressed independently of color images.
  auto d=depthImages.find(depthKey);if(d!=depthImages.end()){depth=d->second.first;depthView=d->second.second;}
 }
 void copyFeedback() {
  context->OMSetRenderTargets(0,nullptr,nullptr);
  ID3D11ShaderResourceView* empty=nullptr;context->PSSetShaderResources(2,1,&empty);
  resolve();
  if(!feedback) {
   D3D11_TEXTURE2D_DESC desc;color->GetDesc(&desc);desc.BindFlags=D3D11_BIND_SHADER_RESOURCE;
   check(device->CreateTexture2D(&desc,nullptr,&feedback),"Create framebuffer feedback");check(device->CreateShaderResourceView(feedback.Get(),nullptr,&feedbackView),"Create framebuffer SRV");
  }
  context->CopyResource(feedback.Get(),samples>1?color.Get():multisample.Get());
  ID3D11ShaderResourceView* view=feedbackView.Get();context->PSSetShaderResources(2,1,&view);
 }
 ~Renderer() { if(context) { context->ClearState(); context->Flush(); } }
 void size(int w,int h) {
  if(w<1||h<1||w>7680||h>4320) throw std::runtime_error("Invalid Direct3D render size");
  if(w==width&&h==height) return;
  context->OMSetRenderTargets(0,nullptr,nullptr); target.Reset(); color.Reset(); depthView.Reset(); depth.Reset(); staging.Reset();
  if(swap) check(swap->ResizeBuffers(0,w,h,DXGI_FORMAT_UNKNOWN,0),"ResizeBuffers");
  width=w;height=h;
  surfaces.clear();depthImages.clear();depthKey=0;feedback.Reset();feedbackView.Reset();multisample.Reset();currentTarget=0;makeSurface();
 }
 void upload(int id,int w,int h,const void* bytes) {
  if(w<1||h<1||w>4096||h>4096) throw std::runtime_error("Invalid texture dimensions");
  D3D11_TEXTURE2D_DESC desc={};desc.Width=w;desc.Height=h;desc.MipLevels=1;desc.ArraySize=1;desc.Format=DXGI_FORMAT_R8G8B8A8_UNORM;
  desc.SampleDesc.Count=1;desc.BindFlags=D3D11_BIND_SHADER_RESOURCE;desc.Usage=D3D11_USAGE_IMMUTABLE;
  D3D11_SUBRESOURCE_DATA data={};data.pSysMem=bytes;data.SysMemPitch=w*4; Texture texture;
  check(device->CreateTexture2D(&desc,&data,&texture.data),"Upload texture");check(device->CreateShaderResourceView(texture.data.Get(),nullptr,&texture.view),"Create texture SRV");
  textures[id]=std::move(texture);
 }
};
static Renderer& renderer(jlong pointer) { if(!pointer) throw std::runtime_error("Direct3D device is closed");return *reinterpret_cast<Renderer*>(pointer); }
static void failure(JNIEnv* env,const std::exception& error) { env->ThrowNew(env->FindClass("java/lang/IllegalStateException"),error.what()); }
static D3D11_BLEND blend(int mode) {
 switch(mode) { case 0:return D3D11_BLEND_ZERO;case 1:return D3D11_BLEND_ONE;case 768:return D3D11_BLEND_SRC_COLOR;case 769:return D3D11_BLEND_INV_SRC_COLOR;
 case 770:return D3D11_BLEND_SRC_ALPHA;case 771:return D3D11_BLEND_INV_SRC_ALPHA;case 772:return D3D11_BLEND_DEST_ALPHA;case 773:return D3D11_BLEND_INV_DEST_ALPHA;
 case 774:return D3D11_BLEND_DEST_COLOR;case 775:return D3D11_BLEND_INV_DEST_COLOR;default:return D3D11_BLEND_ONE; }
}
static D3D11_TEXTURE_ADDRESS_MODE wrap(int mode) { return mode==10497?D3D11_TEXTURE_ADDRESS_WRAP: mode==33648?D3D11_TEXTURE_ADDRESS_MIRROR:D3D11_TEXTURE_ADDRESS_CLAMP; }
static void draw(Renderer& r,const float* data,size_t count,int primitive,const int* s,const float* c) {
 size_t bytes=count*13*sizeof(float); if(!count)return;
 if(c[36]>0.5f)r.copyFeedback();
 if(bytes>r.vertexBytes) {
  r.vertices.Reset();r.vertexBytes=(bytes+65535)&~size_t(65535);D3D11_BUFFER_DESC desc={};desc.ByteWidth=(UINT)r.vertexBytes;desc.Usage=D3D11_USAGE_DYNAMIC;desc.BindFlags=D3D11_BIND_VERTEX_BUFFER;desc.CPUAccessFlags=D3D11_CPU_ACCESS_WRITE;
  check(r.device->CreateBuffer(&desc,nullptr,&r.vertices),"Create vertex buffer");
 }
 D3D11_MAPPED_SUBRESOURCE map={};check(r.context->Map(r.vertices.Get(),0,D3D11_MAP_WRITE_DISCARD,0,&map),"Map vertices");memcpy(map.pData,data,bytes);r.context->Unmap(r.vertices.Get(),0);
 r.context->UpdateSubresource(r.constants.Get(),0,nullptr,c,0,0);
 auto dkey=(unsigned long long)s[0]|((unsigned long long)s[1]<<1)|((unsigned long long)s[2]<<8);
 auto& ds=r.depths[dkey];if(!ds){D3D11_DEPTH_STENCIL_DESC desc={};desc.DepthEnable=s[0]!=0;desc.DepthWriteMask=s[1]?D3D11_DEPTH_WRITE_MASK_ALL:D3D11_DEPTH_WRITE_MASK_ZERO;desc.DepthFunc=(D3D11_COMPARISON_FUNC)(s[2]-511);check(r.device->CreateDepthStencilState(&desc,&ds),"Create depth state");}
 r.context->OMSetDepthStencilState(ds.Get(),0);
 auto bkey=(unsigned long long)s[3]|((unsigned long long)s[4]<<8)|((unsigned long long)s[5]<<24)|((unsigned long long)(c[36]<0)<<48);
 auto& bs=r.blends[bkey];if(!bs){D3D11_BLEND_DESC desc={};auto& t=desc.RenderTarget[0];t.BlendEnable=s[3]!=0;t.RenderTargetWriteMask=c[36]<0?0:D3D11_COLOR_WRITE_ENABLE_ALL;
 t.SrcBlend=blend(s[4]);t.DestBlend=blend(s[5]);t.BlendOp=D3D11_BLEND_OP_ADD;t.SrcBlendAlpha= s[4]==768?D3D11_BLEND_SRC_ALPHA:s[4]==769?D3D11_BLEND_INV_SRC_ALPHA:s[4]==774?D3D11_BLEND_DEST_ALPHA:s[4]==775?D3D11_BLEND_INV_DEST_ALPHA:blend(s[4]);
 t.DestBlendAlpha=s[5]==768?D3D11_BLEND_SRC_ALPHA:s[5]==769?D3D11_BLEND_INV_SRC_ALPHA:s[5]==774?D3D11_BLEND_DEST_ALPHA:s[5]==775?D3D11_BLEND_INV_DEST_ALPHA:blend(s[5]);t.BlendOpAlpha=D3D11_BLEND_OP_ADD;
 check(r.device->CreateBlendState(&desc,&bs),"Create blend state");}r.context->OMSetBlendState(bs.Get(),nullptr,0xFFFFFFFF);
 auto rkey=(unsigned long long)s[6]|((unsigned long long)s[11]<<1)|((unsigned long long)s[12]<<8)|((unsigned long long)(c[35]>0)<<24);
 auto& rs=r.rasters[rkey];if(!rs){D3D11_RASTERIZER_DESC desc={};desc.FillMode=s[11]?D3D11_FILL_WIREFRAME:D3D11_FILL_SOLID;desc.CullMode=s[12]==1028?D3D11_CULL_FRONT:s[12]==1029?D3D11_CULL_BACK:D3D11_CULL_NONE;
 desc.DepthBias=c[35]>0?-3:0;desc.SlopeScaledDepthBias=c[35]>0?-3.0f:0.0f;desc.FrontCounterClockwise=TRUE;desc.ScissorEnable=s[6]!=0;desc.DepthClipEnable=TRUE;desc.MultisampleEnable=TRUE;check(r.device->CreateRasterizerState(&desc,&rs),"Create rasterizer state");}r.context->RSSetState(rs.Get());
 D3D11_VIEWPORT viewport={(FLOAT)s[13],(FLOAT)(r.height-s[14]-s[16]),(FLOAT)s[15],(FLOAT)s[16],0,1};r.context->RSSetViewports(1,&viewport);
 D3D11_RECT rect={s[7],r.height-s[8]-s[10],s[7]+s[9],r.height-s[8]};r.context->RSSetScissorRects(1,&rect);
 for(int i=0;i<2;i++) {
  auto it=r.textures.find(s[17+i]);ID3D11ShaderResourceView* view=it==r.textures.end()?nullptr:it->second.view.Get();r.context->PSSetShaderResources(i,1,&view);
  int index=19+i*3;auto key=(unsigned long long)s[index]|((unsigned long long)s[index+1]<<16)|((unsigned long long)s[index+2]<<32);
  auto& sampler=r.samplers[key];if(!sampler){D3D11_SAMPLER_DESC desc={};desc.Filter=s[index]==9728?D3D11_FILTER_MIN_MAG_MIP_POINT:D3D11_FILTER_MIN_MAG_MIP_LINEAR;desc.AddressU=wrap(s[index+1]);desc.AddressV=wrap(s[index+2]);desc.AddressW=D3D11_TEXTURE_ADDRESS_CLAMP;desc.MaxLOD=D3D11_FLOAT32_MAX;desc.ComparisonFunc=D3D11_COMPARISON_NEVER;check(r.device->CreateSamplerState(&desc,&sampler),"Create sampler");}
  ID3D11SamplerState* pointer=sampler.Get();r.context->PSSetSamplers(i,1,&pointer);
 }
 ID3D11RenderTargetView* target=r.target.Get();r.context->OMSetRenderTargets(1,&target,r.depthView.Get());
 UINT stride=13*sizeof(float),offset=0;ID3D11Buffer* vb=r.vertices.Get();r.context->IASetVertexBuffers(0,1,&vb,&stride,&offset);r.context->IASetInputLayout(r.layout.Get());
 r.context->IASetPrimitiveTopology(primitive==1?D3D11_PRIMITIVE_TOPOLOGY_LINELIST:D3D11_PRIMITIVE_TOPOLOGY_TRIANGLELIST);
 r.context->VSSetShader(r.vertexShader.Get(),nullptr,0);r.context->PSSetShader(r.pixelShader.Get(),nullptr,0);ID3D11Buffer* cb=r.constants.Get();r.context->PSSetConstantBuffers(0,1,&cb);r.context->Draw((UINT)count,0);
}

extern "C" JNIEXPORT jlong JNICALL Java_d3d11_NativeD3D11_create(JNIEnv* env,jclass,jobject canvas,jint width,jint height,jboolean warpDevice) {
 Renderer* r=nullptr;
 try {
  HWND window=nullptr;
  if(canvas) {JAWT awt={};awt.version=JAWT_VERSION_1_4;if(!JAWT_GetAWT(env,&awt))throw std::runtime_error("JAWT is unavailable");
   JAWT_DrawingSurface* surface=awt.GetDrawingSurface(env,canvas);if(!surface)throw std::runtime_error("Canvas is not displayable");
   jint result=surface->Lock(surface);if(result&JAWT_LOCK_ERROR){awt.FreeDrawingSurface(surface);throw std::runtime_error("Cannot lock canvas");}
   JAWT_DrawingSurfaceInfo* info=surface->GetDrawingSurfaceInfo(surface);if(info)window=((JAWT_Win32DrawingSurfaceInfo*)info->platformInfo)->hwnd;
   if(info)surface->FreeDrawingSurfaceInfo(info);surface->Unlock(surface);awt.FreeDrawingSurface(surface);if(!window)throw std::runtime_error("Canvas has no HWND");
  }
  r=new Renderer();D3D_FEATURE_LEVEL level;D3D_FEATURE_LEVEL levels[]={D3D_FEATURE_LEVEL_11_0};
  HRESULT result=D3D11CreateDevice(nullptr,warpDevice?D3D_DRIVER_TYPE_WARP:D3D_DRIVER_TYPE_HARDWARE,nullptr,D3D11_CREATE_DEVICE_BGRA_SUPPORT,levels,1,D3D11_SDK_VERSION,&r->device,&level,&r->context);
  if(FAILED(result)&&!warpDevice){r->software=true;result=D3D11CreateDevice(nullptr,D3D_DRIVER_TYPE_WARP,nullptr,D3D11_CREATE_DEVICE_BGRA_SUPPORT,levels,1,D3D11_SDK_VERSION,&r->device,&level,&r->context);}else r->software=warpDevice;
  check(result,"Create Direct3D 11 device");
  for(int count: {8,4,2}) { UINT colorQuality=0,depthQuality=0;r->device->CheckMultisampleQualityLevels(DXGI_FORMAT_R8G8B8A8_UNORM,count,&colorQuality);r->device->CheckMultisampleQualityLevels(DXGI_FORMAT_D24_UNORM_S8_UINT,count,&depthQuality);if(colorQuality&&depthQuality){r->samples=count;break;} }
  if(window){ComPtr<IDXGIDevice> dxgi;check(r->device.As(&dxgi),"Query DXGI device");ComPtr<IDXGIAdapter> adapter;check(dxgi->GetAdapter(&adapter),"Query adapter");ComPtr<IDXGIFactory> factory;check(adapter->GetParent(IID_PPV_ARGS(&factory)),"Query DXGI factory");
   DXGI_SWAP_CHAIN_DESC desc={};desc.BufferDesc.Width=width;desc.BufferDesc.Height=height;desc.BufferDesc.Format=DXGI_FORMAT_R8G8B8A8_UNORM;desc.SampleDesc.Count=1;desc.BufferUsage=DXGI_USAGE_RENDER_TARGET_OUTPUT;desc.BufferCount=2;desc.OutputWindow=window;desc.Windowed=TRUE;desc.SwapEffect=DXGI_SWAP_EFFECT_DISCARD;
   check(factory->CreateSwapChain(r->device.Get(),&desc,&r->swap),"Create swap chain");factory->MakeWindowAssociation(window,DXGI_MWA_NO_ALT_ENTER);
  }
  ComPtr<ID3DBlob> vs,ps,error;
  HRESULT hr=D3DCompile(shader,strlen(shader),"KernelN64 N64 shaders",nullptr,nullptr,"vs","vs_5_0",D3DCOMPILE_OPTIMIZATION_LEVEL3,0,&vs,&error);if(FAILED(hr))throw std::runtime_error(error?(char*)error->GetBufferPointer():"Vertex shader compilation failed");
  error.Reset();hr=D3DCompile(shader,strlen(shader),"KernelN64 N64 shaders",nullptr,nullptr,"ps","ps_5_0",D3DCOMPILE_OPTIMIZATION_LEVEL3,0,&ps,&error);if(FAILED(hr))throw std::runtime_error(error?(char*)error->GetBufferPointer():"Pixel shader compilation failed");
  check(r->device->CreateVertexShader(vs->GetBufferPointer(),vs->GetBufferSize(),nullptr,&r->vertexShader),"Create VS");check(r->device->CreatePixelShader(ps->GetBufferPointer(),ps->GetBufferSize(),nullptr,&r->pixelShader),"Create PS");
  D3D11_INPUT_ELEMENT_DESC elements[]={{"POSITION",0,DXGI_FORMAT_R32G32B32A32_FLOAT,0,0,D3D11_INPUT_PER_VERTEX_DATA,0},{"COLOR",0,DXGI_FORMAT_R32G32B32A32_FLOAT,0,16,D3D11_INPUT_PER_VERTEX_DATA,0},{"TEXCOORD",0,DXGI_FORMAT_R32G32_FLOAT,0,32,D3D11_INPUT_PER_VERTEX_DATA,0},{"TEXCOORD",1,DXGI_FORMAT_R32G32_FLOAT,0,40,D3D11_INPUT_PER_VERTEX_DATA,0},{"TEXCOORD",2,DXGI_FORMAT_R32_FLOAT,0,48,D3D11_INPUT_PER_VERTEX_DATA,0}};
  check(r->device->CreateInputLayout(elements,5,vs->GetBufferPointer(),vs->GetBufferSize(),&r->layout),"Create vertex layout");
  D3D11_BUFFER_DESC desc={};desc.ByteWidth=52*sizeof(float);desc.BindFlags=D3D11_BIND_CONSTANT_BUFFER;check(r->device->CreateBuffer(&desc,nullptr,&r->constants),"Create shader constants");
  r->size(width,height);return reinterpret_cast<jlong>(r);
 } catch(const std::exception& error){delete r;failure(env,error);return 0;}
}
extern "C" JNIEXPORT void JNICALL Java_d3d11_NativeD3D11_close(JNIEnv*,jclass,jlong pointer){delete reinterpret_cast<Renderer*>(pointer);}
extern "C" JNIEXPORT void JNICALL Java_d3d11_NativeD3D11_resize(JNIEnv* env,jclass,jlong pointer,jint w,jint h){try{renderer(pointer).size(w,h);}catch(const std::exception& error){failure(env,error);}}
extern "C" JNIEXPORT jstring JNICALL Java_d3d11_NativeD3D11_description(JNIEnv* env,jclass,jlong pointer){try{return env->NewStringUTF(renderer(pointer).software?"Direct3D 11 (WARP software device)":"Direct3D 11 (hardware device)");}catch(const std::exception& error){failure(env,error);return nullptr;}}
extern "C" JNIEXPORT void JNICALL Java_d3d11_NativeD3D11_clear(JNIEnv* env,jclass,jlong pointer,jint mask,jfloat red,jfloat green,jfloat blue,jfloat alpha){try{auto& r=renderer(pointer);float color[]={red,green,blue,alpha};if(mask&0x4000)r.context->ClearRenderTargetView(r.target.Get(),color);if(mask&0x100)r.context->ClearDepthStencilView(r.depthView.Get(),D3D11_CLEAR_DEPTH,1,0);}catch(const std::exception& error){failure(env,error);}}
extern "C" JNIEXPORT void JNICALL Java_d3d11_NativeD3D11_texture(JNIEnv* env,jclass,jlong pointer,jint id,jint w,jint h,jobject buffer){try{void* data=env->GetDirectBufferAddress(buffer);if(!data||env->GetDirectBufferCapacity(buffer)<(jlong)w*h*4)throw std::runtime_error("Invalid texture upload buffer");renderer(pointer).upload(id,w,h,data);}catch(const std::exception& error){failure(env,error);}}
extern "C" JNIEXPORT void JNICALL Java_d3d11_NativeD3D11_deleteTexture(JNIEnv* env,jclass,jlong pointer,jint id){try{renderer(pointer).textures.erase(id);}catch(const std::exception& error){failure(env,error);}}
extern "C" JNIEXPORT void JNICALL Java_d3d11_NativeD3D11_draw(JNIEnv* env,jclass,jlong pointer,jfloatArray vertexArray,jint primitive,jintArray stateArray,jfloatArray constantArray){
 std::vector<float> vertices(env->GetArrayLength(vertexArray)),constants(env->GetArrayLength(constantArray));std::vector<int> state(env->GetArrayLength(stateArray));
 env->GetFloatArrayRegion(vertexArray,0,(jsize)vertices.size(),vertices.data());env->GetFloatArrayRegion(constantArray,0,(jsize)constants.size(),constants.data());env->GetIntArrayRegion(stateArray,0,(jsize)state.size(),state.data());
 try{if(state.size()!=25||constants.size()!=52||vertices.size()%13)throw std::runtime_error("Invalid draw command");auto& r=renderer(pointer);size_t count=vertices.size()/13;
 if(constants[36]>0.5f && primitive!=1 && ((int)constants[37]& (64|128))!=0) {
  for(size_t i=0;i+2<count;i+=3)draw(r,vertices.data()+i*13,3,primitive,state.data(),constants.data());
 }else draw(r,vertices.data(),count,primitive,state.data(),constants.data());}catch(const std::exception& error){failure(env,error);}
}
extern "C" JNIEXPORT void JNICALL Java_d3d11_NativeD3D11_present(JNIEnv* env,jclass,jlong pointer,jboolean vsync){try{auto& r=renderer(pointer);r.resolve();if(r.swap){ComPtr<ID3D11Texture2D> back;check(r.swap->GetBuffer(0,IID_PPV_ARGS(&back)),"Get swap buffer");r.context->CopyResource(back.Get(),r.color.Get());check(r.swap->Present(vsync?1:0,0),"Present");}else r.context->Flush();}catch(const std::exception& error){failure(env,error);}}
extern "C" JNIEXPORT jbyteArray JNICALL Java_d3d11_NativeD3D11_read(JNIEnv* env,jclass,jlong pointer){try{auto& r=renderer(pointer);r.resolve();if(!r.staging){D3D11_TEXTURE2D_DESC desc;r.color->GetDesc(&desc);desc.Usage=D3D11_USAGE_STAGING;desc.BindFlags=0;desc.CPUAccessFlags=D3D11_CPU_ACCESS_READ;check(r.device->CreateTexture2D(&desc,nullptr,&r.staging),"Create readback texture");}
 r.context->CopyResource(r.staging.Get(),r.color.Get());D3D11_MAPPED_SUBRESOURCE map;check(r.context->Map(r.staging.Get(),0,D3D11_MAP_READ,0,&map),"Read frame");
 std::vector<jbyte> pixels((size_t)r.width*r.height*4);for(int y=0;y<r.height;y++)memcpy(pixels.data()+(size_t)y*r.width*4,(char*)map.pData+(size_t)y*map.RowPitch,(size_t)r.width*4);r.context->Unmap(r.staging.Get(),0);
 jbyteArray result=env->NewByteArray((jsize)pixels.size());env->SetByteArrayRegion(result,0,(jsize)pixels.size(),pixels.data());return result;
 }catch(const std::exception& error){failure(env,error);return nullptr;}}
extern "C" JNIEXPORT void JNICALL Java_d3d11_NativeD3D11_image(JNIEnv* env,jclass,jlong pointer,jint w,jint h,jobject buffer){try{auto& r=renderer(pointer);void* data=env->GetDirectBufferAddress(buffer);if(!data||env->GetDirectBufferCapacity(buffer)<(jlong)w*h*4)throw std::runtime_error("Invalid preview buffer");r.upload(-1,w,h,data);
 float vertices[]={-1,1,0,1,1,1,1,1,0,0,0,0,0, 1,1,0,1,1,1,1,1,1,0,0,0,0, 1,-1,0,1,1,1,1,1,1,1,0,0,0, -1,1,0,1,1,1,1,1,0,0,0,0,0, 1,-1,0,1,1,1,1,1,1,1,0,0,0, -1,-1,0,1,1,1,1,1,0,1,0,0,0};
 int state[25]={};state[2]=519;state[15]=r.width;state[16]=r.height;state[17]=-1;state[19]=9728;state[20]=state[21]=33071;state[22]=9728;state[23]=state[24]=33071;
 float constants[52]={};constants[12]=1;constants[13]=519;constants[16]=constants[17]=constants[18]=20;constants[19]=1;constants[20]=constants[21]=constants[22]=20;constants[23]=9;
 draw(r,vertices,6,4,state,constants);
 }catch(const std::exception& error){failure(env,error);}}

extern "C" JNIEXPORT jint JNICALL Java_d3d11_NativeD3D11_samples(JNIEnv* env,jclass,jlong pointer){try{return renderer(pointer).samples;}catch(const std::exception& error){failure(env,error);return 1;}}
extern "C" JNIEXPORT void JNICALL Java_d3d11_NativeD3D11_selectTarget(JNIEnv* env,jclass,jlong pointer,jint key){try{renderer(pointer).select(key);}catch(const std::exception& error){failure(env,error);}}
extern "C" JNIEXPORT void JNICALL Java_d3d11_NativeD3D11_forgetTarget(JNIEnv* env,jclass,jlong pointer,jint key){try{auto& r=renderer(pointer);r.surfaces.erase(key);}catch(const std::exception& error){failure(env,error);}}

extern "C" JNIEXPORT void JNICALL Java_d3d11_NativeD3D11_selectDepth(JNIEnv* env,jclass,jlong pointer,jint key){try{renderer(pointer).selectDepth(key);}catch(const std::exception& error){failure(env,error);}}
extern "C" JNIEXPORT void JNICALL Java_d3d11_NativeD3D11_resetTargets(JNIEnv* env,jclass,jlong pointer){try{auto& r=renderer(pointer);r.context->OMSetRenderTargets(0,nullptr,nullptr);r.surfaces.clear();r.depthImages.clear();r.currentTarget=0;r.depthKey=0;r.color.Reset();r.multisample.Reset();r.depth.Reset();r.target.Reset();r.depthView.Reset();r.makeSurface();}catch(const std::exception& error){failure(env,error);}}

extern "C" JNIEXPORT void JNICALL Java_d3d11_NativeD3D11_clearDepthRect(JNIEnv* env,jclass,jlong pointer,jint x,jint y,jint w,jint h,jfloat value) {
 try {
  auto& r=renderer(pointer);float left=x*2.0f/r.width-1,right=(x+w)*2.0f/r.width-1,top=1-y*2.0f/r.height,bottom=1-(y+h)*2.0f/r.height,z=value*2-1;
  float vertices[78]={};float positions[12]={left,top,right,top,right,bottom,left,top,right,bottom,left,bottom};
  for(int i=0;i<6;i++){vertices[i*13]=positions[i*2];vertices[i*13+1]=positions[i*2+1];vertices[i*13+2]=z;vertices[i*13+3]=1;}
  int state[25]={};state[0]=state[1]=1;state[2]=519;state[15]=r.width;state[16]=r.height;state[19]=state[22]=9728;
  float constants[52]={};constants[12]=1;constants[13]=519;constants[36]=-1;
  draw(r,vertices,6,4,state,constants);
 }catch(const std::exception& error){failure(env,error);}
}
