package shaders;

import static org.lwjgl.opengl.GL20.*;

public class BaseFragmentShader implements Shaderable {
    private int fragmentShaderID;
    @Override
    public void setup(int shaderProgram) {
        // compile shaders
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
        
            out vec4 FragColor;
            
            uniform vec3 color;
        
            void main() {
                FragColor = vec4(color, 1.0);
            }
        """;
    }
}
