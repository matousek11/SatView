package shaders;

import static org.lwjgl.opengl.GL20.*;

public class PickingVertexShader implements Shaderable {
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
            layout(location = 0) in vec3 aPos;
        
            uniform mat4 model;
            uniform mat4 view;
            uniform mat4 projection;
            uniform int objectID;
        
            out vec3 fragColor;
        
            void main() {
                gl_Position = projection * view * model * vec4(aPos, 1.0);
                // color for picking
                vec3 color = vec3(0.0);
                if (objectID > 0) {
                    // Encode the ID across all three color channels
                    // R channel: bits 0-7
                    // G channel: bits 8-15
                    // B channel: bits 16-23
                    color.r = float(objectID & 0xFF) / 255.0;
                    color.g = float((objectID >> 8) & 0xFF) / 255.0;
                    color.b = float((objectID >> 16) & 0xFF) / 255.0;
                }
                fragColor = color;
            }
        """;
    }
} 