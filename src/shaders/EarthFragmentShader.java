package shaders;

public class EarthFragmentShader extends BaseFragmentShader {
    @Override
    public String getSource() {
        return """
            #version 330 core
        
            in vec3 vertexColor;
            in vec2 texCoord;
        
            out vec4 FragColor;
        
            uniform sampler2D earthTexture;
        
            void main() {
                // Sample the texture with the given texture coordinates
                vec4 texColor = texture(earthTexture, texCoord);
                // Use the sampled texture color as the final fragment color
                FragColor = texColor;
            }
        """;
    }
}
