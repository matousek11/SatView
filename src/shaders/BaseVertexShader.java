package shaders;

import static org.lwjgl.opengl.GL20.*;

public class BaseVertexShader implements Shaderable {
    private int vertexShaderId;

    @Override
    public void setup(int shaderProgram) {
        // Compile shaders
        vertexShaderId = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertexShaderId, getSource());
        glCompileShader(vertexShaderId);
        if (glGetShaderi(vertexShaderId, GL_COMPILE_STATUS) == GL_FALSE) {
            System.err.println("Shader compile error:\n" + glGetShaderInfoLog(vertexShaderId));
        }

        glAttachShader(shaderProgram, vertexShaderId);
    }

    public void deleteShader() {
        glDeleteShader(vertexShaderId);
    }

    @Override
    public String getSource() {
        return """
            #version 330 core
            layout(location = 0) in vec3 aPos;
            
            uniform mat4 model;
            uniform mat4 view;
            uniform mat4 projection;
            uniform vec3 color;

            void main() {
                gl_Position = projection * view * model * vec4(aPos, 1.0);
            }
        """;
    }
}
