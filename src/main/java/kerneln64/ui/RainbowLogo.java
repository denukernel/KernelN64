package kerneln64.ui;

import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;
import javax.media.opengl.*;
import javax.media.opengl.glu.*;

/** Extruded sketch-shaped K64 geometry with blue face and side shading. */
public final class RainbowLogo {
    private final List<double[]> triangles=new ArrayList<>();
    private final List<List<double[]>> contours=new ArrayList<>();
    /** Hand-built silhouette following the supplied angular K, tall looped 6 and open 4. */
    private static Shape silhouette(){
        Area model=new Area(new Rectangle2D.Double(140,75,55,357));
        model.add(new Area(polygon(161,164,309,57,365,133,216,240)));
        model.add(new Area(polygon(161,248,227,204,365,415,299,461)));
        Path2D six=new Path2D.Double();six.moveTo(430,67);
        six.curveTo(388,45,365,100,350,155);
        six.curveTo(330,219,328,270,333,318);
        six.curveTo(337,392,378,439,438,440);
        six.curveTo(504,448,536,426,536,377);
        six.lineTo(536,345);six.curveTo(534,309,509,276,477,276);six.lineTo(375,276);
        model.add(new Area(new BasicStroke(42,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND).createStrokedShape(six)));
        model.add(new Area(polygon(531,66,586,50,586,197,570,344,617,343,635,335,
            714,335,716,82,729,73,764,72,764,266,752,332,762,331,762,492,752,534,
            695,536,695,421,683,404,551,403,531,386,527,298,527,171)));
        return model;
    }
    private static Shape polygon(double... points){
        Path2D p=new Path2D.Double();p.moveTo(points[0],points[1]);
        for(int i=2;i<points.length;i+=2)p.lineTo(points[i],points[i+1]);p.closePath();return p;
    }
    public static double rotationDegrees(double seconds){return ((seconds%3+3)%3)*120;}
    public RainbowLogo(){
        Shape shape=silhouette();
        Rectangle2D bounds=shape.getBounds2D();double scale=3.5/bounds.getHeight();
        PathIterator path=shape.getPathIterator(null,0.8);double[] p=new double[6];List<double[]> contour=null;
        while(!path.isDone()){
            int type=path.currentSegment(p);
            if(type==PathIterator.SEG_MOVETO){contour=new ArrayList<>();contours.add(contour);}
            if(type==PathIterator.SEG_MOVETO||type==PathIterator.SEG_LINETO)
                contour.add(new double[]{(p[0]-bounds.getCenterX())*scale,(bounds.getCenterY()-p[1])*scale,0});
            path.next();
        }
        GLU glu=new GLU();GLUtessellator tess=glu.gluNewTess();
        GLUtessellatorCallbackAdapter callback=new GLUtessellatorCallbackAdapter(){
            final List<double[]> vertices=new ArrayList<>();int mode;
            @Override public void begin(int primitive){vertices.clear();mode=primitive;}
            @Override public void vertex(Object value){
                vertices.add((double[])value);int n=vertices.size();
                if(mode==GL.GL_TRIANGLES&&n%3==0)add(vertices.get(n-3),vertices.get(n-2),vertices.get(n-1));
                else if(mode==GL.GL_TRIANGLE_FAN&&n>=3)add(vertices.get(0),vertices.get(n-2),vertices.get(n-1));
                else if(mode==GL.GL_TRIANGLE_STRIP&&n>=3){if(n%2==0)add(vertices.get(n-2),vertices.get(n-3),vertices.get(n-1));else add(vertices.get(n-3),vertices.get(n-2),vertices.get(n-1));}
            }
            @Override public void combine(double[] coordinates,Object[] data,float[] weights,Object[] result){result[0]=coordinates.clone();}
            @Override public void error(int error){throw new IllegalStateException("logo tessellation: "+glu.gluErrorString(error));}
        };
        try{
            for(int event:new int[]{GLU.GLU_TESS_BEGIN,GLU.GLU_TESS_VERTEX,GLU.GLU_TESS_END,GLU.GLU_TESS_COMBINE,GLU.GLU_TESS_ERROR})glu.gluTessCallback(tess,event,callback);
            glu.gluTessProperty(tess,GLU.GLU_TESS_WINDING_RULE,GLU.GLU_TESS_WINDING_ODD);glu.gluTessNormal(tess,0,0,1);
            glu.gluTessBeginPolygon(tess,null);
            for(var edge:contours){glu.gluTessBeginContour(tess);for(double[] vertex:edge)glu.gluTessVertex(tess,vertex,0,vertex);glu.gluTessEndContour(tess);}
            glu.gluTessEndPolygon(tess);
        }finally{glu.gluDeleteTess(tess);}
        if(triangles.isEmpty())throw new IllegalStateException("empty logo mesh");
    }
    private void add(double[] a,double[] b,double[] c){triangles.add(a);triangles.add(b);triangles.add(c);}
    private static void vertex(GL gl,double[] p,double z,double time,float brightness){
        gl.glColor3f(0,0.63f*brightness,0.90f*brightness);gl.glVertex3d(p[0],p[1],z);
    }
    public void render(GL gl,int width,int height,double time){
        if(width<=0||height<=0)return;
        gl.glViewport(0,0,width,height);gl.glClearColor(0,0,0,1);gl.glClear(GL.GL_COLOR_BUFFER_BIT|GL.GL_DEPTH_BUFFER_BIT);
        gl.glEnable(GL.GL_DEPTH_TEST);gl.glDepthFunc(GL.GL_LEQUAL);gl.glDisable(GL.GL_CULL_FACE);gl.glDisable(GL.GL_LIGHTING);gl.glDisable(GL.GL_TEXTURE_2D);
        gl.glMatrixMode(GL.GL_PROJECTION);gl.glLoadIdentity();double aspect=width/(double)height;
        double halfHeight=Math.max(2.4,3.9/aspect);gl.glOrtho(-halfHeight*aspect,halfHeight*aspect,-halfHeight,halfHeight,-15,15);
        gl.glMatrixMode(GL.GL_MODELVIEW);gl.glLoadIdentity();gl.glTranslated(0,Math.sin(time*.75)*.10,0);
        gl.glRotated(-10,1,0,0);gl.glRotated(rotationDegrees(time),0,1,0);
        gl.glBegin(GL.GL_TRIANGLES);for(double[] p:triangles)vertex(gl,p,.28,time,1);for(double[] p:triangles)vertex(gl,p,-.28,time,.65f);gl.glEnd();
        gl.glBegin(GL.GL_QUADS);
        for(var edge:contours)for(int i=0;i<edge.size();i++){
            double[] a=edge.get(i),b=edge.get((i+1)%edge.size());
            vertex(gl,a,.28,time,.70f);vertex(gl,b,.28,time,.70f);vertex(gl,b,-.28,time,.40f);vertex(gl,a,-.28,time,.40f);
        }
        gl.glEnd();gl.glFlush();
    }
}
