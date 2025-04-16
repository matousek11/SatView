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
import shaders.BaseFragmentShader;
import shaders.BaseVertexShader;
import shaders.EarthFragmentShader;
import shaders.EarthVertexShader;
import utils.ControlsUtil;

public class Main {
    private long window;
    private ControlsUtil controlsUtil;
    private double prevTime = 0, rotation = 0;
    private int earthRadius = 6378; // in km


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
        window = glfwCreateWindow(1200, 900, "SatView", NULL, NULL);
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
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        float[] satellitePositions = {
          20, 0, 500,
          0, 49, 480,
          0, 0, 125
        };

        float size = 0.03f;
        float[] quadVertices = {
                // Triangle 1
                0.0f,  size,  size,
                0.0f,  size, -size,
                0.0f, -size, -size,

                // Triangle 2
                0.0f,  size,  size,
                0.0f, -size, -size,
                0.0f, -size,  size
        };


        int lineVao = glGenVertexArrays();
        glBindVertexArray(lineVao);

        int lineVbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, lineVbo);
        glBufferData(GL_ARRAY_BUFFER, quadVertices, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(0);


        // setup shaders
        int baseShaderID = glCreateProgram();
        BaseVertexShader baseVertexShader = new BaseVertexShader();
        baseVertexShader.setup(baseShaderID);
        BaseFragmentShader baseFragmentShader = new BaseFragmentShader();
        baseFragmentShader.setup(baseShaderID);

        glLinkProgram(baseShaderID);

        baseVertexShader.deleteShader();
        baseFragmentShader.deleteShader();

        int earthShaderID = glCreateProgram();
        EarthVertexShader earthVertexShader = new EarthVertexShader();
        earthVertexShader.setup(earthShaderID);
        EarthFragmentShader earthFragmentShader = new EarthFragmentShader();
        earthFragmentShader.setup(earthShaderID);

        glLinkProgram(earthShaderID);

        earthVertexShader.deleteShader();
        earthFragmentShader.deleteShader();

        Sphere sphere = new Sphere(40, 40);
        int textureID = sphere.loadTexture();

        // Get uniform locations for transformations
        int modelLocEarth = glGetUniformLocation(earthShaderID, "model");
        int viewLocEarth = glGetUniformLocation(earthShaderID, "view");
        int projLocEarth = glGetUniformLocation(earthShaderID, "projection");

        int modelLocBase = glGetUniformLocation(baseShaderID, "model");
        int projLocBase = glGetUniformLocation(baseShaderID, "projection");

        // Rendering loop
        while (!glfwWindowShouldClose(window)) {
            controlsUtil.processInput();
            glEnable(GL_DEPTH_TEST);
            glDepthFunc(GL_LESS);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            glUseProgram(earthShaderID);

            renderSatellites(modelLocBase, projLocBase, lineVao, satellitePositions);
            renderEarth(earthShaderID, modelLocEarth, viewLocEarth, projLocEarth, textureID, sphere);

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    private void renderEarth(int shaderID, int modelLoc, int viewLoc, int projLoc, int textureID, Sphere sphere) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            double crntTime = glfwGetTime();
            if (crntTime - prevTime >= 1 / 60)
            {
                rotation += 0.001f;
                prevTime = crntTime;
            }

            // Set up transformations
            Matrix4f model = new Matrix4f();
            model.identity();
            model.rotate((float) rotation, new Vector3f(0, 1, 0));
            Matrix4f projection = new Matrix4f();
            projection.setPerspective((float) Math.toRadians(45.0), 800.0f / 600.0f, 0.1f, 100.0f);

            FloatBuffer modelBuffer = model.get(stack.mallocFloat(16));
            FloatBuffer projBuffer = projection.get(stack.mallocFloat(16));

            glUniformMatrix4fv(modelLoc, false, modelBuffer);
            glUniformMatrix4fv(projLoc, false, projBuffer);
        }

        glUniform1i(glGetUniformLocation(shaderID, "earthTexture"), 0);
        glActiveTexture(GL_TEXTURE0); // Activate texture unit 0
        glBindTexture(GL_TEXTURE_2D, textureID);
        controlsUtil.updateViewMatrix(viewLoc);

        sphere.render();
    }

    private void renderSatellites(int modelLoc, int projLoc, int lineVao, float[] positions) {
        for (int i = 0; i <= positions.length - 3; i += 3) {
            float longitude = positions[i];
            float latitude = positions[i + 1];
            float height = positions[i + 2];
            // longitude, latitude, height above earth
            renderPlane(
                    modelLoc,
                    projLoc,
                    lineVao,
                    new Vector3f(((-1/(float)earthRadius) * (earthRadius + height)),0, 0),
                    longitude,
                    latitude
            );
        }
    }

    private void renderPlane(int modelLoc, int projLoc, int lineVao, Vector3f position, float longitude, float latitude) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            Matrix4f model = new Matrix4f();
            model.identity();

            model.rotate((float) Math.toRadians(longitude), 0,1,0);
            model.rotate(-(float) Math.toRadians(latitude), 0,0,1);
            model.translate(position);

            Matrix4f projection = new Matrix4f();
            projection.setPerspective((float) Math.toRadians(45.0), 800.0f / 600.0f, 0.1f, 100.0f);

            FloatBuffer modelBuffer = model.get(stack.mallocFloat(16));
            FloatBuffer projBuffer = projection.get(stack.mallocFloat(16));

            glUniformMatrix4fv(modelLoc, false, modelBuffer);
            glUniformMatrix4fv(projLoc, false, projBuffer);
        }

        glBindVertexArray(lineVao);
        glDrawArrays(GL_TRIANGLES, 0, 6);
        glBindVertexArray(0);
    }

    /**
     * Cleanup resources after end of loop
     */
    private void cleanup() {
        /*glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo);
        glDeleteProgram(shaderProgram);*/
        glfwDestroyWindow(window);
        glfwTerminate();
    }
}
