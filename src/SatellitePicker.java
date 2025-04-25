import objects.Sphere;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;
import shaders.PickingFragmentShader;
import shaders.PickingVertexShader;
import utils.ControlsUtil;
import utils.DataProvider;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

import static java.sql.Types.NULL;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class SatellitePicker {
    private int pickingFBO;
    private int pickingTexture;
    private int pickingRenderBuffer;
    private int pickingShaderID;
    private int windowWidth;
    private int windowHeight;
    private int selectedSatelliteID;
    private DataProvider dataProvider;
    private ArrayList<Integer> satelliteIDs;
    private int earthRadius;
    private Sphere satelliteSphere;

    public SatellitePicker(int windowWidth, int windowHeight, DataProvider dataProvider, ArrayList<Integer> satelliteIDs, int earthRadius) {
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;
        this.dataProvider = dataProvider;
        this.satelliteIDs = satelliteIDs;
        this.earthRadius = earthRadius;
        this.selectedSatelliteID = -1;
        this.satelliteSphere = new Sphere(8, 8);
        
        setupPickingFramebuffer();
        setupPickingShader();
    }

    private void setupPickingFramebuffer() {
        // Create framebuffer
        pickingFBO = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, pickingFBO);

        // Create texture for color attachment
        pickingTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, pickingTexture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, windowWidth, windowHeight, 0, GL_RGBA, GL_UNSIGNED_BYTE, NULL);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, pickingTexture, 0);

        // Create renderbuffer for depth attachment
        pickingRenderBuffer = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, pickingRenderBuffer);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, windowWidth, windowHeight);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, pickingRenderBuffer);

        // Check if framebuffer is complete
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            System.err.println("Framebuffer is not complete!");
        }

        // Unbind framebuffer
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    private void setupPickingShader() {
        // Create picking shader program
        pickingShaderID = glCreateProgram();

        // Create and setup shaders
        PickingVertexShader pickingVertexShader = new PickingVertexShader();
        pickingVertexShader.setup(pickingShaderID);
        PickingFragmentShader pickingFragmentShader = new PickingFragmentShader();
        pickingFragmentShader.setup(pickingShaderID);

        // Link program
        glLinkProgram(pickingShaderID);

        // Delete shaders
        pickingVertexShader.deleteShader();
        pickingFragmentShader.deleteShader();
    }

    public void pickSatellite(double mouseX, double mouseY, ControlsUtil controlsUtil, double satelliteTime) {
        // Save current viewport
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer viewport = stack.mallocInt(4);
            glGetIntegerv(GL_VIEWPORT, viewport);

            // Render to picking framebuffer
            glBindFramebuffer(GL_FRAMEBUFFER, pickingFBO);
            glViewport(0, 0, windowWidth, windowHeight);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            // Use picking shader
            glUseProgram(pickingShaderID);

            // Get uniform locations
            int modelLoc = glGetUniformLocation(pickingShaderID, "model");
            int viewLoc = glGetUniformLocation(pickingShaderID, "view");
            int projLoc = glGetUniformLocation(pickingShaderID, "projection");
            int objectIDLoc = glGetUniformLocation(pickingShaderID, "objectID");

            controlsUtil.updateViewMatrix(viewLoc);

            Matrix4f projection = new Matrix4f();
            projection.setPerspective((float) Math.toRadians(45.0), (float)windowWidth / windowHeight, 0.1f, 100.0f);
            FloatBuffer projBuffer = projection.get(stack.mallocFloat(16));
            glUniformMatrix4fv(projLoc, false, projBuffer);

            // Enable depth testing for picking
            glEnable(GL_DEPTH_TEST);
            glDepthFunc(GL_LESS);

            // Render satellites with unique colors
            for (Integer satelliteID : satelliteIDs) {
                float[] data = dataProvider.getRecord(satelliteID, (int)satelliteTime);
                if (data.length == 0) continue;

                float longitude = data[0];
                float latitude = data[1];
                float height = data[2];

                // Set object ID uniform
                glUniform1i(objectIDLoc, satelliteID);

                // Render satellite as a sphere
                renderPickingSphere(
                        modelLoc,
                        new Vector3f(((-1 / (float) earthRadius) * (earthRadius + height)), 0, 0),
                        longitude,
                        latitude
                );
            }

            // Read pixel color
            ByteBuffer pixelBuffer = stack.malloc(4);
            glReadPixels((int)mouseX, windowHeight - (int)mouseY - 1, 1, 1, GL_RGBA, GL_UNSIGNED_BYTE, pixelBuffer);

            // Convert color back to object ID by combining all three channels
            int r = pixelBuffer.get(0) & 0xFF;
            int g = pixelBuffer.get(1) & 0xFF;
            int b = pixelBuffer.get(2) & 0xFF;
            int objectID = r | (g << 8) | (b << 16);

            // Check if we hit a satellite
            if (objectID > 0 && satelliteIDs.contains(objectID)) {
                selectedSatelliteID = objectID;
            } else {
                selectedSatelliteID = -1;
            }

            // Unbind framebuffer and restore viewport
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
            glViewport(viewport.get(0), viewport.get(1), viewport.get(2), viewport.get(3));
        }
    }

    private void renderPickingSphere(int modelLoc, Vector3f position, float longitude, float latitude) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            Matrix4f model = new Matrix4f();
            model.identity();

            model.rotate((float) Math.toRadians(longitude), 0,1,0);
            model.rotate(-(float) Math.toRadians(latitude), 0,0,1);
            model.translate(position);
            
            // Make sphere smaller
            model.scale(0.03f);

            FloatBuffer modelBuffer = model.get(stack.mallocFloat(16));
            glUniformMatrix4fv(modelLoc, false, modelBuffer);
        }

        satelliteSphere.render();
    }

    public int getSelectedSatelliteID() {
        return selectedSatelliteID;
    }

    public void cleanup() {
        glDeleteProgram(pickingShaderID);
        glDeleteFramebuffers(pickingFBO);
        glDeleteTextures(pickingTexture);
        glDeleteRenderbuffers(pickingRenderBuffer);
        satelliteSphere.cleanup();
    }
} 