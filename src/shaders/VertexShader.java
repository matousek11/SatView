package shaders;

import static org.lwjgl.opengl.GL20.*;

public class VertexShader implements Shaderable {
    private int vertexShaderId;

    @Override
    public void setup(int shaderProgram) {
        String vertexShaderSource = """
            #version 330 core
            layout(location = 0) in vec3 aPos;
            layout(location = 1) in vec3 aColor;
        
            out vec3 vertexColor;
        
            uniform mat4 model;
            uniform mat4 view;
            uniform mat4 projection;
        
            void main() {
                gl_Position = projection * view * model * vec4(aPos, 1.0);
                vertexColor = aColor;
            }
        """;

        // Compile shaders
        vertexShaderId = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertexShaderId, vertexShaderSource);
        glCompileShader(vertexShaderId);

        glAttachShader(shaderProgram, vertexShaderId);
    }

    public void deleteShader() {
        glDeleteShader(vertexShaderId);
    }
}
