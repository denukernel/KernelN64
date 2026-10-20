#pragma once

// Shared RDP combiner, fog and coverage shader source.
static const char* shader = R"HLSL(
cbuffer Constants : register(b0) {
 float4 primitiveColor, environmentColor, fogColor, options;
 float4 mux[4];
 float4 fogOptions;
 float4 rdpOptions, blender0, blender1, blendColor;
 float4 lodScale, lodOptions;
};
Texture2D texture0 : register(t0); Texture2D texture1 : register(t1);
Texture2D memoryColor : register(t2);
SamplerState sampler0 : register(s0); SamplerState sampler1 : register(s1);
struct Vertex { float4 position:POSITION; float4 color:COLOR0; float2 uv0:TEXCOORD0; float2 uv1:TEXCOORD1; float fog:TEXCOORD2; };
struct Fragment { float4 position:SV_POSITION; float4 color:COLOR0; float2 uv0:TEXCOORD0; float2 uv1:TEXCOORD1; noperspective float fog:TEXCOORD2; };
Fragment vs(Vertex v) {
 Fragment f; f.position=v.position; f.position.z=(v.position.z+v.position.w)*0.5;
 f.color=v.color; f.uv0=v.uv0; f.uv1=v.uv1; f.fog=v.fog; return f;
}
float4 source(int id, float4 combined, float4 t0, float4 t1, float4 shade, float noise, float lodFraction) {
 switch(id) {
 case 0:return combined; case 1:return t0; case 2:return t1; case 3:return primitiveColor;
 case 4:return shade; case 5:return environmentColor; case 6:return 0.5; case 7:return 0.5;
 case 8:return combined.aaaa; case 9:return t0.aaaa; case 10:return t1.aaaa;
 case 11:return primitiveColor.aaaa; case 12:return shade.aaaa; case 13:return environmentColor.aaaa;
 case 14:return lodFraction; case 15:return fogOptions.z; case 16:return noise; case 17:return 0; case 18:return 0;
 case 19:return 1; default:return 0;
 }
}
float3 blendSource(int id, float3 pixel, float3 memory) {
 return id==0?pixel:id==1?memory:id==2?blendColor.rgb:fogColor.rgb;
}
struct Output { float4 color:SV_TARGET; uint mask:SV_Coverage; };
Output ps(Fragment f, uint mask:SV_Coverage) {
 Output output; output.mask=mask;
 if(rdpOptions.w>0.5)f.color.a=f.fog;
 float4 t0=texture0.Sample(sampler0,f.uv0), t1=texture1.Sample(sampler1,f.uv1), result=0;
 float noise=frac(sin(dot(f.position.xy,float2(12.9898,78.233)))*43758.5453);
 float lodFraction=0;
 // Uniform gating keeps derivatives coherent and avoids work on other materials.
 if(lodOptions.x>0.5) {
  float2 texels=f.uv0*lodScale.xy;
  float2 dx=abs(ddx(texels))*lodScale.z,dy=abs(ddy(texels))*lodScale.w;
  lodFraction=saturate(max(max(dx.x,dx.y),max(dy.x,dy.y))-1);
 }
 [unroll] for(int cycle=0;cycle<2;cycle++) {
  if(cycle>0 && options.x<1.5) break;
  float4 rgb=mux[cycle*2], alpha=mux[cycle*2+1];
  float3 color=(source((int)rgb.x,result,t0,t1,f.color,noise,lodFraction).rgb-source((int)rgb.y,result,t0,t1,f.color,noise,lodFraction).rgb)*source((int)rgb.z,result,t0,t1,f.color,noise,lodFraction).rgb+source((int)rgb.w,result,t0,t1,f.color,noise,lodFraction).rgb;
  float a=(source((int)alpha.x,result,t0,t1,f.color,noise,lodFraction).a-source((int)alpha.y,result,t0,t1,f.color,noise,lodFraction).a)*source((int)alpha.z,result,t0,t1,f.color,noise,lodFraction).a+source((int)alpha.w,result,t0,t1,f.color,noise,lodFraction).a;
  result=saturate(float4(color,a));
 }
 int op=(int)options.y; float a=result.a, ref=options.z;
 if(op==512 || (op==513 && !(a<ref)) || (op==514 && !(a==ref)) || (op==515 && !(a<=ref)) || (op==516 && !(a>ref)) || (op==517 && !(a!=ref)) || (op==518 && !(a>=ref))) discard;
 if(rdpOptions.x>0.5) {
  uint mode=(uint)rdpOptions.y;
  float4 memory=memoryColor.Load(int3(f.position.xy,0));
  uint samples=(uint)rdpOptions.z;
  uint incoming=(uint)round(countbits(mask)*8.0/samples);
  if((mode&8)==0) incoming=8;
  float coverageAlpha=result.a;
  if((mode&4096)!=0) incoming=(uint)round(incoming*result.a);
  if(incoming==0) discard;
  if((mode&8192)!=0) result.a=incoming/8.0;
  // Alpha-to-coverage controls the actual multisample writes as well as alpha.
  if((mode&4096)!=0) {
   uint wanted=(uint)round(countbits(mask)*coverageAlpha), selected=0, used=0;
   [unroll] for(uint i=0;i<8;i++) if((mask&(1u<<i))!=0 && used<wanted) { selected|=1u<<i;used++; }
   output.mask=selected;
  }
  uint stored=(mode&64)!=0?(uint)round(memory.a*7.0):7;
  bool blending=(mode&16384)!=0 || ((mode&8)!=0 && incoming<8);
  uint dest=(mode>>8)&3, sum=incoming+stored;
  uint coverage=dest==0?min(blending?sum:incoming-1,7u):dest==1?(sum&7u):dest==2?7u:stored;
  if((mode&128)!=0 && sum<8) result.rgb=memory.rgb;
  else if((mode&16384)!=0 || options.w>0.5) {
   float3 pixel=result.rgb;
   [unroll] for(int cycle=0;cycle<2;cycle++) {
    if(cycle>0 && (options.x<1.5 || (mode&16384)==0)) break;
    float4 b=cycle==0?blender0:blender1;
    float fa=(int)b.y==0?result.a:(int)b.y==1?f.fog:(int)b.y==2?f.color.a:0;
    float fb=(int)b.w==0?1-fa:(int)b.w==1?stored/8.0:(int)b.w==2?1:0;
    // In two-cycle mode the first blend feeds the second blender's pixel input.
    pixel=(blendSource((int)b.x,pixel,memory.rgb)*fa+blendSource((int)b.z,pixel,memory.rgb)*fb)/max(fa+fb,0.00001);
   }
   result.rgb=saturate(pixel);
  }
  result.a=coverage/7.0;
 } else if(options.w>0.5) result.rgb=lerp(result.rgb,fogColor.rgb,saturate(f.fog));
 output.color=result; return output;
}
)HLSL";
