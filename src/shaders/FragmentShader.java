package shaders;

import static org.lwjgl.opengl.GL20.*;

public class FragmentShader implements Shaderable {
    private int fragmentShaderID;
    @Override
    public void setup(int shaderProgram) {
        String fragmentShaderSource = """
            #version 330 core
            in vec3 vertexColor;
            out vec4 FragColor;
            void main() {
                FragColor = vec4(vertexColor, 1.0); // Output the color
            }
        """;

        fragmentShaderID = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragmentShaderID, fragmentShaderSource);
        glCompileShader(fragmentShaderID);

        glAttachShader(shaderProgram, fragmentShaderID);
    }

    public void deleteShader() {
        glDeleteShader(fragmentShaderID);
    }
}
