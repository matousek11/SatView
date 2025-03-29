import objects.Sphere;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static java.sql.Types.NULL;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import org.joml.Matrix4f;
import shaders.FragmentShader;
import shaders.VertexShader;
import utils.ControlsUtil;

public class Main {
    private long window;
    private ControlsUtil controlsUtil;


    public static void main(String[] args) {
        new Main().run();
    }

    /**
     * Startup SatView app
     */
    private void run() {
        init();
        loop();
        cleanup();
    }

    private void init() {
        // Initialize GLFW
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        // Configure GLFW (set OpenGL version, profile, forward compatibility and if window is resizable)
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 1);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        // Create a window
        window = glfwCreateWindow(800, 600, "SatView", NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }

        // Center the window
        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);
            glfwGetWindowSize(window, pWidth, pHeight);
            GLFWVidMode videoMode = glfwGetVideoMode(glfwGetPrimaryMonitor());

            assert videoMode != null;
            glfwSetWindowPos(
                    window,
                    (videoMode.width() - pWidth.get(0)) / 2,
                    (videoMode.height() - pHeight.get(0)) / 2
            );
        }

        controlsUtil = new ControlsUtil(window);
        controlsUtil.registerMouseMovementEventHandler();

        // Make OpenGL context current
        glfwMakeContextCurrent(window);
        glfwSwapInterval(1); // Enable V-Sync
        glfwShowWindow(window);
    }

    /**
     * Run loop in which whole app renders
     */
    private void loop() {
        // Initialize OpenGL bindings
        GL.createCapabilities();

        // Set clear color
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        // setup shaders
        int shaderProgram = glCreateProgram();
        VertexShader vertexShader = new VertexShader();
        vertexShader.setup(shaderProgram);
        FragmentShader fragmentShader = new FragmentShader();
        fragmentShader.setup(shaderProgram);

        glLinkProgram(shaderProgram);

        vertexShader.deleteShader();
        fragmentShader.deleteShader();

        float[] vertices = {
                // Position          // Color
                0.0f,  0.5f, 0.0f,   1.0f, 0.0f, 0.0f, // Red vertex
                -0.5f, -0.5f, 0.0f,  0.0f, 1.0f, 0.0f, // Green vertex
                0.5f, -0.5f, 0.0f,   0.0f, 0.0f, 1.0f  // Blue vertex
        };

        // Create VAO and VBO
        int vao = glGenVertexArrays();
        glBindVertexArray(vao);
        int vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

        // Set vertex attribute pointers
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 6 * Float.BYTES, 0); // Position
        glVertexAttribPointer(1, 3, GL_FLOAT, false, 6 * Float.BYTES, 3 * Float.BYTES); // Color
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);

        /*Sphere sphere = new Sphere(20, 20);*/


        // Get uniform locations for transformations
        int modelLoc = glGetUniformLocation(shaderProgram, "model");
        int viewLoc = glGetUniformLocation(shaderProgram, "view");
        int projLoc = glGetUniformLocation(shaderProgram, "projection");

        float rotation = 0;
        double prevTime = 0;

        // Rendering loop
        while (!glfwWindowShouldClose(window)) {
            controlsUtil.processInput();
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            try (MemoryStack stack = MemoryStack.stackPush()) {
                double crntTime = glfwGetTime();
                if (crntTime - prevTime >= 1 / 60)
                {
                    rotation += 0.01f;
                    prevTime = crntTime;
                }

                // Set up transformations
                Matrix4f model = new Matrix4f();
                model.identity();
                model.rotate(rotation, new Vector3f(0, 1, 0));
                Matrix4f view = new Matrix4f();
                view.identity();
                view.translate(new Vector3f(0.0f, 0.0f, -2.0f)); // Camera position
                Matrix4f projection = new Matrix4f();
                projection.setPerspective((float) Math.toRadians(45.0), 800.0f / 600.0f, 0.1f, 100.0f);

                FloatBuffer modelBuffer = model.get(stack.mallocFloat(16));
                FloatBuffer viewBuffer = view.get(stack.mallocFloat(16));
                FloatBuffer projBuffer = projection.get(stack.mallocFloat(16));

                glUniformMatrix4fv(modelLoc, false, modelBuffer);
                glUniformMatrix4fv(viewLoc, false, viewBuffer);
                glUniformMatrix4fv(projLoc, false, projBuffer);
            }

            glUseProgram(shaderProgram);
            controlsUtil.updateViewMatrix(viewLoc);
            glBindVertexArray(vao);
            glDrawArrays(GL_TRIANGLES, 0, 3);


            /*Matrix4f sphereModel = new Matrix4f().translate(1.5f, 0.0f, 0.0f);
            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer modelBuffer = stack.mallocFloat(16);
                sphereModel.get(modelBuffer);
                glUniformMatrix4fv(modelLoc, false, modelBuffer);
            }*/


            /*sphere.render();*/

            glfwSwapBuffers(window);
            glfwPollEvents();
            /*sphere.cleanup();*/
        }
    }

    /**
     * Cleanup resources after end of loop
     */
    private void cleanup() {
        // Free resources
        /*glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo);
        glDeleteProgram(shaderProgram);*/
        glfwDestroyWindow(window);
        glfwTerminate();
    }
}
