package shaders;

import static org.lwjgl.opengl.GL20.*;

public class TextVertexShader implements Shaderable {
    private int vertexShaderId;

    @Override
    public void setup(int shaderProgram) {
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
            layout(location = 0) in vec2 aPos;
            layout(location = 1) in vec2 aUV;

            uniform vec2 uScreenSize;

            out vec2 vUV;
            void main() {
                vec2 ndc = (aPos / uScreenSize) * 2.0 - 1.0;
                gl_Position = vec4(ndc.x, -ndc.y, 0, 1);
                vUV = aUV;
            }
        """;
    }
} 