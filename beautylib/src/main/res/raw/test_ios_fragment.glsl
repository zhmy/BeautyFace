varying highp vec2 textureCoordinate;
varying highp vec2 textureCoordinate2;

 uniform sampler2D inputImageTexture;
 uniform sampler2D inputImageTexture2;
 uniform mediump float smoothDegree;

 void main() {
     highp vec4 bilateral = texture2D(inputImageTexture, textureCoordinate);
     highp vec4 origin = texture2D(inputImageTexture2, textureCoordinate2);
     highp vec4 smooth1;
     lowp float r = origin.r;
     lowp float g = origin.g;
     lowp float b = origin.b;
     if (r > 0.3725 && g > 0.1568 && b > 0.0784 && r > b && (max(max(r, g), b) - min(min(r, g), b)) > 0.0588 && abs(r-g) > 0.0588) {
         smooth1 = (1.0 - smoothDegree) * (origin - bilateral) + bilateral;
     }
     else {
         smooth1 = origin;
     }
     smooth1.r = log(1.0 + 0.2 * smooth1.r)/log(1.2);
     smooth1.g = log(1.0 + 0.2 * smooth1.g)/log(1.2);
     smooth1.b = log(1.0 + 0.2 * smooth1.b)/log(1.2);
     gl_FragColor = smooth1;
 }




