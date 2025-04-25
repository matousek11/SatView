package shaders;

import static org.lwjgl.opengl.GL20.*;

public class PickingFragmentShader implements Shaderable {
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
            in vec3 fragColor;
            out vec4 FragColor;
        
            void main() {
                FragColor = vec4(fragColor, 1.0);
            }
        """;
    }
} 