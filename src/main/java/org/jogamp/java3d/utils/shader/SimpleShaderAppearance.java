/*
 * Copyright (c) 2016 JogAmp Community. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are those
 * of the authors and should not be interpreted as representing official policies,
 * either expressed or implied, of the JogAmp Community.
 *
 */
package org.jogamp.java3d.utils.shader;

import java.util.HashMap;

import org.jogamp.java3d.ColoringAttributes;
import org.jogamp.java3d.GLSLShaderProgram;
import org.jogamp.java3d.LineAttributes;
import org.jogamp.java3d.Material;
import org.jogamp.java3d.NodeComponent;
import org.jogamp.java3d.PointAttributes;
import org.jogamp.java3d.PolygonAttributes;
import org.jogamp.java3d.RenderingAttributes;
import org.jogamp.java3d.Shader;
import org.jogamp.java3d.ShaderAppearance;
import org.jogamp.java3d.ShaderAttributeSet;
import org.jogamp.java3d.ShaderAttributeValue;
import org.jogamp.java3d.SourceCodeShader;
import org.jogamp.java3d.TexCoordGeneration;
import org.jogamp.java3d.Texture;
import org.jogamp.java3d.TextureAttributes;
import org.jogamp.java3d.TextureUnitState;
import org.jogamp.java3d.TransparencyAttributes;
import org.jogamp.vecmath.Color3f;
import org.jogamp.vecmath.Vector4f;

/**
 * SimpleShaderAppearance is a mechanism to quickly build shaders compatible with Java3D 1.7.0 Jogl2es2Pipeline.
 * It will use the set components to determine the shader to use, for example if you set a material
 * then it will add the lighting uniforms values and do the calculations
 * Similarly if you set a texture it will sample from it.
 * 
 * Sharing SimpleShaderAppearances (such as appears to be done by Box) will almost certainly cause problems, however the shader programs
 * that are identical are automatically shared internally.
 * 
 * If you want to see examples of the shader source use getVertexShaderSource and getFragmentShaderSource after setting up
 * a SimpleShaderAppearance as you would have done for a regular Appearance
 * 
 * Some pipeline data such as FogData are not used by this class and must be manually setup if desired.
 * 
 * To use the auto builder simply construct using SimpleShaderAppearance()
 * To force a flat shader use SimpleShaderAppearance(false, false)
 * To force a colored line shader use SimpleShaderAppearance(new Color4f(1,0,1,1,))
 * 
 * Note, this defaults to the values for desktop, on ES hardware you must call
 * SimpleShaderAppearance.setVersionES300();
 * or SimpleShaderAppearance.setVersionES100();
 * @author phil
 *
 */
public class SimpleShaderAppearance extends ShaderAppearance
{

	private static String versionString = "#version 120\n";
	private static String outString = "varying";
	private static String inString = "varying";
	private static String fragColorDec = "";
	private static String fragColorVar = "gl_FragColor";
	private static String vertexAttributeInString = "attribute";
	private static String texture2D = "texture2D";

	public static void setVersionES100()
	{
		versionString = "#version 100\n";
		outString = "varying";
		inString = "varying";
		fragColorDec = "";
		fragColorVar = "gl_FragColor";
		vertexAttributeInString = "attribute";
		texture2D = "texture2D";
	}

	public static void setVersionES300()
	{
		versionString = "#version 300 es\n";
		outString = "out";
		inString = "in";
		fragColorDec = "out vec4 glFragColor;\n";
		fragColorVar = "glFragColor";
		vertexAttributeInString = "in";
		texture2D = "texture";
	}

	public static void setVersion120()
	{
		versionString = "#version 120\n";
		outString = "varying";
		inString = "varying";
		fragColorDec = "";
		fragColorVar = "gl_FragColor";
		vertexAttributeInString = "attribute";
		texture2D = "texture2D";
	}

	public static String alphaTestUniforms = "uniform int alphaTestEnabled;\n" + //
			"uniform int alphaTestFunction;\n" + //
			"uniform float alphaTestValue;\n";

	public static String alphaTestMethod = "if(alphaTestEnabled != 0)\n" + //
			"{	\n" + //
			" 	if(alphaTestFunction==516)//>\n" + //
			"		if(baseMap.a<=alphaTestValue)discard;\n" + //
			"	else if(alphaTestFunction==518)//>=\n" + //
			"		if(baseMap.a<alphaTestValue)discard;\n" + //
			"	else if(alphaTestFunction==514)//==\n" + //
			"		if(baseMap.a!=alphaTestValue)discard;\n" + //
			"	else if(alphaTestFunction==517)//!=\n" + //
			"		if(baseMap.a==alphaTestValue)discard;\n" + //
			"	else if(alphaTestFunction==513)//<\n" + //
			"		if(baseMap.a>=alphaTestValue)discard;\n" + //
			"	else if(alphaTestFunction==515)//<=\n" + //
			"		if(baseMap.a>alphaTestValue)discard;\n" + //
			"	else if(alphaTestFunction==512)//never	\n" + //
			"		discard;	\n" + //
			"}\n";

	public static String glFrontMaterial = "struct material\n" + //
			"	{\n" + //
			"		int lightEnabled;\n" + //
			"		vec4 ambient;\n" + //
			"		vec4 diffuse;\n" + //
			"		vec4 emission; \n" + //
			"		vec3 specular;\n" + //
			"		float shininess;\n" + //
			"	};\n" + //
			"uniform material glFrontMaterial;\n"; //

	public static String glLightSource = "struct lightSource\n" + //
			"	{\n" + //
			"		vec4 position;\n" + //
			"		vec4 diffuse;\n" + //
			"		vec4 specular;\n" + //
			"		float constantAttenuation, linearAttenuation, quadraticAttenuation;\n" + //
			"		float spotCutoff, spotExponent;\n" + //
			"		vec3 spotDirection;\n" + //
			"	};\n" + //
			"\n" + //
			"	uniform int numberOfLights;\n" + //
			"	const int maxLights = 3;\n" + //
			"	uniform lightSource glLightSource[maxLights];\n"; //

	private static HashMap<Integer, GLSLShaderProgram> shaderPrograms = new HashMap<Integer, GLSLShaderProgram>();

	private static GLSLShaderProgram flatShaderProgram;
	private static GLSLShaderProgram colorLineShaderProgram;

	private static HashMap<GLSLShaderProgram, String> vertexShaderSources = new HashMap<GLSLShaderProgram, String>();
	private static HashMap<GLSLShaderProgram, String> fragmentShaderSources = new HashMap<GLSLShaderProgram, String>();

	private boolean buildBasedOnAttributes = false;

	// we can't set it in the super class as tex coord gen is not supported in the pipeline
	// so we record it in this class when set
	private TexCoordGeneration texCoordGeneration = null;

	private String vertexShaderSource = null;

	private String fragmentShaderSource = null;

	/**
	 * This will define the shader code based on the attributes set
	 */
	public SimpleShaderAppearance()
	{
		buildBasedOnAttributes = true;
		rebuildShaders();
	}

	/**
	 * Lines with a single color no texture, ignores vertex attribute of color
	 * @param color
	 */
	public SimpleShaderAppearance(Color3f color)
	{
		this(color, false, false);
	}

	public SimpleShaderAppearance(boolean lit, boolean hasTexture)
	{
		this(null, lit, hasTexture);
	}

	/** if color is not null then a line appearance
	 * otherwise simple poly appearance
	 * @param color
	 */
	private SimpleShaderAppearance(Color3f color, boolean lit, boolean hasTexture)
	{
		if (lit || hasTexture)
		{
			boolean hasTextureCoordGen = false;
			boolean texCoordGenModeObjLinear = false;
			build(hasTexture, lit, hasTextureCoordGen, texCoordGenModeObjLinear);
		}
		else
		{
			if (color != null)
			{
				PolygonAttributes polyAtt = new PolygonAttributes(PolygonAttributes.POLYGON_LINE, PolygonAttributes.CULL_NONE, 0.0f);
				polyAtt.setPolygonOffset(0.1f);
				setPolygonAttributes(polyAtt);
				LineAttributes lineAtt = new LineAttributes(1, LineAttributes.PATTERN_SOLID, false);
				setLineAttributes(lineAtt);

				ColoringAttributes colorAtt = new ColoringAttributes(color, ColoringAttributes.FASTEST);
				setColoringAttributes(colorAtt);

				RenderingAttributes ra = new RenderingAttributes();
				ra.setIgnoreVertexColors(true);
				setRenderingAttributes(ra);

				Material mat = new Material();
				setMaterial(mat);

				if (colorLineShaderProgram == null)
				{
					colorLineShaderProgram = new GLSLShaderProgram() {
						@Override
						public String toString()
						{
							return "SimpleShaderAppearance colorLineShaderProgram";
						}
					};
					String vertexProgram = versionString;
					vertexProgram += vertexAttributeInString + " vec4 glVertex;\n";
					vertexProgram += vertexAttributeInString + " vec4 glColor;\n";
					vertexProgram += "uniform int ignoreVertexColors;\n";
					vertexProgram += "uniform vec4 objectColor;\n";
					vertexProgram += "uniform mat4 glModelViewProjectionMatrix;\n";
					vertexProgram += outString + " vec4 glFrontColor;\n";
					vertexProgram += "void main( void ){\n";
					vertexProgram += "gl_Position = glModelViewProjectionMatrix * glVertex;\n";
					vertexProgram += "if( ignoreVertexColors != 0 )\n";
					vertexProgram += "	glFrontColor = objectColor;\n";
					vertexProgram += "else\n";
					vertexProgram += "	glFrontColor = glColor;\n";
					vertexProgram += "}";

					String fragmentProgram = versionString;
					fragmentProgram += "precision mediump float;\n";
					fragmentProgram += inString + " vec4 glFrontColor;\n";
					fragmentProgram += fragColorDec;
					fragmentProgram += "void main( void ){\n";
					fragmentProgram += fragColorVar + " = glFrontColor;\n";
					fragmentProgram += "}";

					colorLineShaderProgram.setShaders(makeShaders(vertexProgram, fragmentProgram));

					vertexShaderSources.put(colorLineShaderProgram, vertexProgram);
					fragmentShaderSources.put(colorLineShaderProgram, fragmentProgram);
				}

				setShaderProgram(colorLineShaderProgram);
				vertexShaderSource = vertexShaderSources.get(colorLineShaderProgram);
				fragmentShaderSource = fragmentShaderSources.get(colorLineShaderProgram);

			}
			else
			{

				if (flatShaderProgram == null)
				{
					flatShaderProgram = new GLSLShaderProgram() {
						@Override
						public String toString()
						{
							return "SimpleShaderAppearance flatShaderProgram";
						}
					};
					String vertexProgram = versionString;
					vertexProgram += vertexAttributeInString + " vec4 glVertex;\n";
					vertexProgram += vertexAttributeInString + " vec4 glColor;\n";
					vertexProgram += "uniform int ignoreVertexColors;\n";
					vertexProgram += "uniform vec4 objectColor;\n";
					vertexProgram += "uniform mat4 glModelViewProjectionMatrix;\n";
					vertexProgram += outString + " vec4 glFrontColor;\n";
					vertexProgram += "void main( void ){\n";
					vertexProgram += "gl_Position = glModelViewProjectionMatrix * glVertex;\n";
					vertexProgram += "if( ignoreVertexColors != 0 )\n";
					vertexProgram += "	glFrontColor = objectColor;\n";
					vertexProgram += "else\n";
					vertexProgram += "	glFrontColor = glColor;\n";
					vertexProgram += "}";

					String fragmentProgram = versionString;
					fragmentProgram += "precision mediump float;\n";
					fragmentProgram += "uniform float transparencyAlpha;\n";
					fragmentProgram += inString + " vec4 glFrontColor;\n";
					fragmentProgram += fragColorDec;
					fragmentProgram += "void main( void ){\n";
					fragmentProgram += fragColorVar + " = glFrontColor;\n";
					fragmentProgram += fragColorVar + ".a *= transparencyAlpha;\n";
					fragmentProgram += "}";

					flatShaderProgram.setShaders(makeShaders(vertexProgram, fragmentProgram));
					vertexShaderSources.put(flatShaderProgram, vertexProgram);
					fragmentShaderSources.put(flatShaderProgram, fragmentProgram);
					//System.out.println("vertexProgram " +vertexProgram);
					//System.out.println("fragmentProgram " +fragmentProgram);

				}

				setShaderProgram(flatShaderProgram);
				vertexShaderSource = vertexShaderSources.get(flatShaderProgram);
				fragmentShaderSource = fragmentShaderSources.get(flatShaderProgram);
			}

		}

	}

	public String getVertexShaderSource()
	{
		return vertexShaderSource;
	}

	public String getFragmentShaderSource()
	{
		return fragmentShaderSource;
	}

	private static Shader[] makeShaders(String vertexProgram, String fragmentProgram)
	{
		Shader[] shaders = new Shader[2];
		shaders[0] = new SourceCodeShader(Shader.SHADING_LANGUAGE_GLSL, Shader.SHADER_TYPE_VERTEX, vertexProgram) {
			@Override
			public String toString()
			{
				return "vertexProgram";
			}
		};
		shaders[1] = new SourceCodeShader(Shader.SHADING_LANGUAGE_GLSL, Shader.SHADER_TYPE_FRAGMENT, fragmentProgram) {
			@Override
			public String toString()
			{
				return "fragmentProgram";
			}
		};
		return shaders;
	}

	private void rebuildShaders()
	{
		if (buildBasedOnAttributes)
		{
			// we only rebuild if we are not yet live or the right capabilities have been set
			if (
			// first check is that the appearance is not live/compiled or if it is the various parts can be got at
			((!this.isLive() && !this.isCompiled()) // appearance is not live/compiled
					|| (this.getCapability(ALLOW_MATERIAL_READ)//
							&& this.getCapability(ALLOW_TEXTURE_UNIT_STATE_READ) //
							&& this.getCapability(ALLOW_TEXTURE_READ)//
							&& this.getCapability(ALLOW_POLYGON_ATTRIBUTES_READ)))//		
					// second part of check is that each component can be live/compiled elsewhere so need checking separately
					&& (this.getPolygonAttributes() == null // no poly attributes
							|| (!this.getPolygonAttributes().isLive() && !this.getPolygonAttributes().isCompiled()) // poly attributes are not yet live
							|| this.getPolygonAttributes().getCapability(PolygonAttributes.ALLOW_MODE_READ))//poly attributes are live but can be read
					
					// finally we must be allowed to set the shader program while live
					&& ((!this.isLive() && !this.isCompiled()) || (this.getCapability(ALLOW_SHADER_PROGRAM_WRITE)))
			)
			{
				boolean hasTexture = this.getTexture() != null || this.getTextureUnitCount() > 0;
				if (this.getTextureUnitCount() > 0)
					System.out.println("this.getTextureUnitCount() " + this.getTextureUnitCount());
				boolean lit = this.getMaterial() != null; // having material== lit geometry

				//POLYGON_LINE and POLYGON_POINT are not lit
				lit = lit && (this.getPolygonAttributes() == null
						|| this.getPolygonAttributes().getPolygonMode() == PolygonAttributes.POLYGON_FILL);
				
				boolean hasTextureCoordGen = hasTexture && texCoordGeneration != null;

				boolean texCoordGenModeObjLinear = hasTextureCoordGen
						&& (texCoordGeneration.getGenMode() == TexCoordGeneration.OBJECT_LINEAR);
				build(hasTexture, lit, hasTextureCoordGen, texCoordGenModeObjLinear);
			}
		}
	}

	private void build(boolean hasTexture, boolean lit, boolean hasTextureCoordGen, boolean texCoordGenModeObjLinear)
	{
		int shaderKey = (hasTexture ? 1 : 0) + (lit ? 2 : 0) + (hasTextureCoordGen ? 4 : 0) + (texCoordGenModeObjLinear ? 8 : 0);

		GLSLShaderProgram shaderProgram = shaderPrograms.get(new Integer(shaderKey));
		if (shaderProgram == null)
		{
			String vertexProgram = versionString;
			String fragmentProgram = versionString;
			if (hasTextureCoordGen)
			{
				if (texCoordGeneration.getFormat() != 0)
					System.out.println("texCoordGeneration.getFormat() must be 0");
				/** 
				 * Generates texture coordinates as a linear function in object coordinates.
				 public static final int OBJECT_LINEAR = 0;				    
				 * Generates texture coordinates as a linear function in eye coordinates.				    
				 public static final int EYE_LINEAR    = 1;				   
				 * Generates texture coordinates using a spherical reflection mapping in eye coordinates.
				 public static final int SPHERE_MAP    = 2;
				 * Generates texture coordinates that match vertices' normals in eye coordinates.
				 public static final int NORMAL_MAP    = 3;
				 * Generates texture coordinates that match vertices' reflection vectors in eye coordinates.
				 public static final int REFLECTION_MAP = 4;
				 see multitex.vert in examples for sphere map and cube map and google for the others
				 */
			}

			if (lit)
			{

				vertexProgram += vertexAttributeInString + " vec4 glVertex;\n";
				vertexProgram += vertexAttributeInString + " vec4 glColor;\n";
				vertexProgram += vertexAttributeInString + " vec3 glNormal; \n";
				if (hasTexture && !hasTextureCoordGen)
				{
					vertexProgram += vertexAttributeInString + " vec2 glMultiTexCoord0;\n";
				}
				vertexProgram += "uniform mat4 glModelViewProjectionMatrix;\n";
				vertexProgram += "uniform mat4 glModelViewMatrix;\n";
				vertexProgram += "uniform mat3 glNormalMatrix;\n";
				vertexProgram += "uniform int ignoreVertexColors;\n";
				vertexProgram += "uniform vec4 glLightModelambient;\n";
				vertexProgram += glFrontMaterial;
				vertexProgram += glLightSource;
				if (hasTextureCoordGen && texCoordGenModeObjLinear)
				{
					vertexProgram += "uniform vec4 texCoordGenPlaneS;\n";
					vertexProgram += "uniform vec4 texCoordGenPlaneT;\n";
				}
				if (hasTexture)
				{
					vertexProgram += outString + " vec2 glTexCoord0;\n";
				}

				vertexProgram += outString + "  vec3 ViewVec;\n";
				vertexProgram += outString + "  vec3 N;\n";
				vertexProgram += outString + "  vec4 A;\n";
				vertexProgram += outString + "  vec4 C;\n";
				vertexProgram += outString + "  vec3 emissive;\n";
				vertexProgram += outString + "  vec4 lightsD[maxLights];\n";
				vertexProgram += outString + "  vec3 lightsS[maxLights];\n";
				vertexProgram += outString + "  vec3 lightsLightDir[maxLights];\n";
				vertexProgram += outString + "  float shininess;\n";
				if (hasTextureCoordGen)
				{
					vertexProgram += "vec2 object_linear(vec4 pos, vec4 planeOS, vec4 planeOT)\n";
					vertexProgram += "{\n";
					vertexProgram += "	return vec2(pos.x*planeOS.x+pos.y*planeOS.y+pos.z*planeOS.z+pos.w*planeOS.w,pos.x*planeOT.x+pos.y*planeOT.y+pos.z*planeOT.z+pos.w*planeOT.w);\n";
					vertexProgram += "}\n";
				}

				vertexProgram += "void main( void ){\n";
				vertexProgram += "gl_Position = glModelViewProjectionMatrix * glVertex;\n";
				vertexProgram += "N = normalize(glNormalMatrix * glNormal);\n";
				if (hasTexture)
				{
					if (!hasTextureCoordGen)
					{
						vertexProgram += "glTexCoord0 = glMultiTexCoord0.st;\n";
					}
					else
					{
						if (texCoordGenModeObjLinear)
						{
							vertexProgram += "glTexCoord0 = object_linear(glVertex, texCoordGenPlaneS, texCoordGenPlaneT);\n";
						}
						else
						{
							System.err.println("texCoordGeneration.getGenMode() not supported " + texCoordGeneration.getGenMode());
						}
					}
				}

				vertexProgram += "vec3 v = vec3(glModelViewMatrix * glVertex);\n";

				vertexProgram += "ViewVec = -v.xyz;\n";

				vertexProgram += "A = glLightModelambient * glFrontMaterial.ambient;\n";
				vertexProgram += "if( ignoreVertexColors != 0) \n";
				// objectColor should be used if it is no lighting, and reusing material diffuse appears wrong
				vertexProgram += "	C = vec4(1,1,1,1);//glFrontMaterial.diffuse; \n";
				vertexProgram += "else \n";
				vertexProgram += "	C = glColor; \n";

				vertexProgram += "emissive = glFrontMaterial.emission.rgb;\n";
				vertexProgram += "shininess = glFrontMaterial.shininess;\n";

				vertexProgram += "for (int index = 0; index < numberOfLights && index < maxLights; index++) // for all light sources\n";
				vertexProgram += "{	\n";
				vertexProgram += "	lightsD[index] = glLightSource[index].diffuse * glFrontMaterial.diffuse;	\n";
				vertexProgram += "	lightsS[index] = glLightSource[index].specular.rgb * glFrontMaterial.specular;\n";
				vertexProgram += "	lightsLightDir[index] = glLightSource[index].position.xyz;	\n";
				vertexProgram += "}\n";
				vertexProgram += "}";

				fragmentProgram += "precision mediump float;\n";
				fragmentProgram += "precision highp int;\n";
				fragmentProgram += "uniform float transparencyAlpha;\n";
				if (hasTexture)
				{
					fragmentProgram += alphaTestUniforms;

					fragmentProgram += inString + " vec2 glTexCoord0;\n";
					fragmentProgram += "uniform sampler2D BaseMap;\n";
				}
				fragmentProgram += "uniform int numberOfLights;\n";
				fragmentProgram += inString + " vec3 ViewVec;\n";

				fragmentProgram += inString + " vec3 N;\n";

				fragmentProgram += inString + " vec4 A;\n";
				fragmentProgram += inString + " vec4 C;\n";

				fragmentProgram += inString + " vec3 emissive;\n";
				fragmentProgram += inString + " float shininess;\n";
				fragmentProgram += " const int maxLights = 3;\n";
				fragmentProgram += inString + " vec4 lightsD[maxLights]; \n";
				fragmentProgram += inString + " vec3 lightsS[maxLights]; \n";
				fragmentProgram += inString + " vec3 lightsLightDir[maxLights]; \n";

				fragmentProgram += fragColorDec;
				fragmentProgram += "void main( void ){\n ";
				if (hasTexture)
				{
					fragmentProgram += "vec4 baseMap = " + texture2D + "( BaseMap, glTexCoord0.st );\n";
				}
				if (hasTexture)
				{
					fragmentProgram += alphaTestMethod;
				}

				fragmentProgram += "vec4 color;\n";
				fragmentProgram += "vec3 albedo = " + (hasTexture ? "baseMap.rgb *" : "") + " C.rgb;\n";

				fragmentProgram += "vec3 diffuse = A.rgb;\n";
				fragmentProgram += "vec3 spec;\n";

				fragmentProgram += "vec3 normal = N;\n";
				fragmentProgram += "vec3 E = normalize(ViewVec);\n";
				fragmentProgram += "float EdotN = max( dot(normal, E), 0.0 );\n";

				fragmentProgram += "for (int index = 0; index < numberOfLights && index < maxLights; index++) // for all light sources\n";
				fragmentProgram += "{ 	\n";
				fragmentProgram += "	vec3 L = normalize( lightsLightDir[index] );\n";
				fragmentProgram += "	//vec3 R = reflect(-L, normal);\n";
				fragmentProgram += "	vec3 H = normalize( L + E );		\n";
				fragmentProgram += "	float NdotL = max( dot(normal, L), 0.0 );\n";
				fragmentProgram += "	float NdotH = max( dot(normal, H), 0.0 );	\n";
				fragmentProgram += "	float NdotNegL = max( dot(normal, -L), 0.0 );	\n";

				fragmentProgram += "	diffuse = diffuse + (lightsD[index].rgb * NdotL);\n";
				fragmentProgram += "	spec = spec + (lightsS[index] * pow(NdotH, 0.3*shininess));\n";
				fragmentProgram += "}\n";

				fragmentProgram += "color.rgb = albedo * (diffuse + emissive) + spec;\n";
				if (hasTexture)
				{
					fragmentProgram += "color.a = C.a * baseMap.a;\n";
				}
				else
				{
					fragmentProgram += "color.a = C.a;\n";
				}

				fragmentProgram += "color.a *= transparencyAlpha;\n";
				fragmentProgram += fragColorVar + " = color;\n";

				//for debug of the incorrect looking tex coord gen values
				//if (hasTexture)
				//fragmentProgram += fragColorVar + " = vec4(mod(glTexCoord0.s,1.0),mod(glTexCoord0.t,1.0),0,1);\n";

				fragmentProgram += "}";

			}
			else
			{
				// not lit
				if (hasTexture)
				{
					vertexProgram += vertexAttributeInString + " vec4 glVertex;\n";
					vertexProgram += vertexAttributeInString + " vec2 glMultiTexCoord0;\n";
					vertexProgram += "uniform mat4 glModelViewProjectionMatrix;\n";
					vertexProgram += outString + " vec2 glTexCoord0;\n";
					vertexProgram += "void main( void ){\n";
					vertexProgram += "gl_Position = glModelViewProjectionMatrix * glVertex;\n";
					vertexProgram += "glTexCoord0 = glMultiTexCoord0.st;\n";
					vertexProgram += "}";

					fragmentProgram += "precision mediump float;\n";
					fragmentProgram += "uniform float transparencyAlpha;\n";
					fragmentProgram += alphaTestUniforms;
					fragmentProgram += inString + " vec2 glTexCoord0;\n";
					fragmentProgram += "uniform sampler2D BaseMap;\n";
					fragmentProgram += fragColorDec;
					fragmentProgram += "void main( void ){\n ";
					fragmentProgram += "vec4 baseMap = " + texture2D + "( BaseMap, glTexCoord0.st );\n";
					fragmentProgram += alphaTestMethod;
					fragmentProgram += "baseMap.a *= transparencyAlpha;\n";
					fragmentProgram += fragColorVar + " = baseMap;\n";
					fragmentProgram += "}";

				}
				else
				{
					//no lit no texture					
					vertexProgram += vertexAttributeInString + " vec4 glVertex;\n";
					vertexProgram += vertexAttributeInString + " vec4 glColor;\n";
					vertexProgram += "uniform int ignoreVertexColors;\n";
					vertexProgram += "uniform vec4 objectColor;\n";
					vertexProgram += "uniform mat4 glModelViewProjectionMatrix;\n";
					vertexProgram += outString + " vec4 glFrontColor;\n";
					vertexProgram += "void main( void ){\n";
					vertexProgram += "gl_Position = glModelViewProjectionMatrix * glVertex;\n";
					vertexProgram += "if( ignoreVertexColors != 0 )\n";
					vertexProgram += "	glFrontColor = objectColor;\n";
					vertexProgram += "else\n";
					vertexProgram += "	glFrontColor = glColor;\n";
					vertexProgram += "}";

					fragmentProgram += "precision mediump float;\n";
					fragmentProgram += "uniform float transparencyAlpha;\n";
					fragmentProgram += inString + " vec4 glFrontColor;\n";
					fragmentProgram += fragColorDec;
					fragmentProgram += "void main( void ){\n";
					fragmentProgram += fragColorVar + " = glFrontColor;\n";
					fragmentProgram += fragColorVar + ".a *= transparencyAlpha;\n";
					fragmentProgram += "}";

				}

			}

			// build the shader program and cache it
			shaderProgram = new GLSLShaderProgram() {
				@Override
				public String toString()
				{
					return "SimpleShaderAppearance " + getName();
				}
			};
			shaderProgram.setName("shaderkey = " + shaderKey);
			shaderProgram.setShaders(makeShaders(vertexProgram, fragmentProgram));
			vertexShaderSources.put(shaderProgram, vertexProgram);
			fragmentShaderSources.put(shaderProgram, fragmentProgram);

			if (hasTexture)
			{
				if (texCoordGenModeObjLinear)
				{
					shaderProgram.setShaderAttrNames(new String[] { "BaseMap", "texCoordGenPlaneS", "texCoordGenPlaneT" });
				}
				else
				{
					shaderProgram.setShaderAttrNames(new String[] { "BaseMap" });
				}
			}
			shaderPrograms.put(new Integer(shaderKey), shaderProgram);

		}

		setShaderProgram(shaderProgram);
		vertexShaderSource = vertexShaderSources.get(shaderProgram);
		fragmentShaderSource = fragmentShaderSources.get(shaderProgram);

		if (hasTexture)
		{
			ShaderAttributeSet shaderAttributeSet = new ShaderAttributeSet();
			shaderAttributeSet.put(new ShaderAttributeValue("BaseMap", new Integer(0)));
			if (texCoordGenModeObjLinear)
			{
				Vector4f planeS = new Vector4f();
				texCoordGeneration.getPlaneS(planeS);
				Vector4f planeT = new Vector4f();
				texCoordGeneration.getPlaneT(planeT);

				shaderAttributeSet.put(new ShaderAttributeValue("texCoordGenPlaneS", planeS));
				shaderAttributeSet.put(new ShaderAttributeValue("texCoordGenPlaneT", planeT));
			}

			setShaderAttributeSet(shaderAttributeSet);
		}

	}

	@Override
	public void setMaterial(Material material)
	{
		super.setMaterial(material);
		rebuildShaders();
	}

	@Override
	public void setColoringAttributes(ColoringAttributes coloringAttributes)
	{
		super.setColoringAttributes(coloringAttributes);
		rebuildShaders();
	}

	@Override
	public void setTransparencyAttributes(TransparencyAttributes transparencyAttributes)
	{
		super.setTransparencyAttributes(transparencyAttributes);
		rebuildShaders();
	}

	@Override
	public void setRenderingAttributes(RenderingAttributes renderingAttributes)
	{
		super.setRenderingAttributes(renderingAttributes);
		rebuildShaders();
	}

	@Override
	public void setPolygonAttributes(PolygonAttributes polygonAttributes)
	{
		super.setPolygonAttributes(polygonAttributes);
		rebuildShaders();
	}

	@Override
	public void setLineAttributes(LineAttributes lineAttributes)
	{
		super.setLineAttributes(lineAttributes);
		rebuildShaders();
	}

	@Override
	public void setPointAttributes(PointAttributes pointAttributes)
	{
		super.setPointAttributes(pointAttributes);
		rebuildShaders();
	}

	@Override
	public void setTexture(Texture texture)
	{
		super.setTexture(texture);
		rebuildShaders();
	}

	@Override
	public void setTextureAttributes(TextureAttributes textureAttributes)
	{
		super.setTextureAttributes(textureAttributes);
		rebuildShaders();
	}

	@Override
	public void setTexCoordGeneration(TexCoordGeneration texCoordGeneration)
	{
		// can't use the super version as it's not supported	
		//super.setTexCoordGeneration(texCoordGeneration);
		this.texCoordGeneration = texCoordGeneration;
		rebuildShaders();
	}

	@Override
	public void setTextureUnitState(TextureUnitState[] stateArray)
	{
		System.out.println("SimpleShaderAppearance with textureunitstates in use");
		super.setTextureUnitState(stateArray);
		rebuildShaders();
	}

	@Override
	public void setTextureUnitState(int index, TextureUnitState state)
	{
		System.out.println("SimpleShaderAppearance with textureunitstates in use");
		super.setTextureUnitState(index, state);
		rebuildShaders();
	}

	/**
	* Must implement or clones turn out to be ShaderAppearance
	*/
	@SuppressWarnings("deprecation")
	@Override
	public NodeComponent cloneNodeComponent()
	{
		SimpleShaderAppearance a = new SimpleShaderAppearance();
		a.duplicateNodeComponent(this);
		return a;
	}
}
