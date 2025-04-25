package shaders;

import static org.lwjgl.opengl.GL20.*;

public class TextFragmentShader implements Shaderable {
    private int fragmentShaderID;

    @Override
    public void setup(int shaderProgram) {
        fragmentShaderID = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragmentShaderID, getSource());
        glCompileShader(fragmentShaderID);
        if (glGetShaderi(fragmentShaderID, GL_COMPILE_STATUS) == GL_FALSE) {
            System.err.println("Shader compile error:\n" + glGetShaderInfoLog(fragmentShaderID));
        }

        glAttachShader(shaderProgram, fragmentShaderID);
    }

    public void deleteShader() {
        glDeleteShader(fragmentShaderID);
    }

    @Override
    public String getSource() {
        return """
            #version 330 core
            in vec2 vUV;
            out vec4 fragColor;
            uniform sampler2D textAtlas;

            void main() {
                float alpha = texture(textAtlas, vUV).r;
                // Smooth the edges slightly for better anti-aliasing
                alpha = smoothstep(0.0, 1.0, alpha);
                fragColor = vec4(0.95, 0.95, 1.0, alpha);
            }
        """;
    }
} 