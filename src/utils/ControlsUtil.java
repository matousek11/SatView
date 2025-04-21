package utils;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;

public class ControlsUtil {
    private final Vector3f cameraPos = new Vector3f(-3.0f, 0.0f, 0.0f);
    private final Vector3f cameraFront = new Vector3f(1.0f, 0.0f, 0.0f);
    private final Vector3f cameraUp = new Vector3f(0.0f, 1.0f, 0.0f);
    private float cameraSpeed = 0.05f;
    private float cameraDistance = 3.0f;
    private final float zoomSensitivity = 0.5f;

    private double lastX, lastY;
    private boolean firstMouse = true;
    private float yaw = 180f;   // Start looking at Europe
    private float pitch = 0.0f;
    private final float mouseSensitivity = 0.2f;
    private double timespeed = 1;

    private final long windowID;
    private boolean leftMouseButtonPressed = false;
    private boolean showInfobox = false;

    public ControlsUtil(long windowID) {
        this.windowID = windowID;
        updateCameraPosition();
        registerScrollCallback();
    }

    private void registerScrollCallback() {
        glfwSetScrollCallback(windowID, (window, xoffset, yoffset) -> {
            cameraDistance = Math.max(1.2f, cameraDistance - (float)yoffset * zoomSensitivity);
            updateCameraPosition();
        });
    }

    public void registerMouseMovementEventHandler() {
        glfwSetCursorPosCallback(windowID, (window, xpos, ypos) -> {
            if (!leftMouseButtonPressed) {
                return;
            }

            if (firstMouse) {
                lastX = xpos;
                lastY = ypos;
                firstMouse = false;
            }

            double dx = xpos - lastX;
            double dy = ypos - lastY;

            lastX = xpos;
            lastY = ypos;

            dx *= mouseSensitivity;
            dy *= mouseSensitivity;

            yaw += (float) dx;
            pitch += (float) dy;

            // Limit pitch to prevent flipping
            if (pitch > 89.0f) pitch = 89.0f;
            if (pitch < -89.0f) pitch = -89.0f;

            updateCameraPosition();
        });
    }

    private void updateCameraPosition() {
        // Calculate new camera position based on spherical coordinates
        float x = (float) (cameraDistance * Math.cos(Math.toRadians(pitch)) * Math.cos(Math.toRadians(yaw)));
        float y = (float) (cameraDistance * Math.sin(Math.toRadians(pitch)));
        float z = (float) (cameraDistance * Math.cos(Math.toRadians(pitch)) * Math.sin(Math.toRadians(yaw)));

        cameraPos.set(x, y, z);
        cameraFront.set(-x, -y, -z).normalize();
    }

    /**
     * Update view matrix in openGL state
     */
    public void updateViewMatrix(int viewLoc) {
        Matrix4f view = new Matrix4f();
        view.lookAt(cameraPos, new Vector3f(0, 0, 0), cameraUp);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer viewBuffer = view.get(stack.mallocFloat(16));
            glUniformMatrix4fv(viewLoc, false, viewBuffer);
        }
    }

    /**
     * Handle user control of app
     */
    public void processInput() {
        // Time speed control
        if (glfwGetKey(windowID, GLFW_KEY_J) == GLFW_PRESS) {
            timespeed -= 1;
        } else if (glfwGetKey(windowID, GLFW_KEY_K) == GLFW_PRESS) {
            timespeed += 1;
        } else if (glfwGetKey(windowID, GLFW_KEY_P) == GLFW_PRESS) {
            timespeed = 0;
        } else if (glfwGetKey(windowID, GLFW_KEY_N) == GLFW_PRESS) {
            timespeed = 1;
        }

        // show infobox
        if (glfwGetKey(windowID, GLFW_KEY_I) == GLFW_PRESS) {
            showInfobox = !showInfobox;
        }

        // keyboard zoom controls as alternative
        if (glfwGetKey(windowID, GLFW_KEY_Z) == GLFW_PRESS) {
            cameraDistance = Math.max(1.0f, cameraDistance - cameraSpeed);
            updateCameraPosition();
        } else if (glfwGetKey(windowID, GLFW_KEY_U) == GLFW_PRESS) {
            cameraDistance += cameraSpeed;
            updateCameraPosition();
        }

        if (glfwGetMouseButton(windowID, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS) {
            leftMouseButtonPressed = true;
        } else {
            firstMouse = true;
            leftMouseButtonPressed = false;
        }
    }

    public double getTimespeed() {
        return timespeed;
    }

    public boolean getShowInfobox() {
        return showInfobox;
    }
}
