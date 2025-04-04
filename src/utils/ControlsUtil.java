package utils;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;

public class ControlsUtil {
    private final Vector3f cameraPos = new Vector3f(-3.0f, 0f, 0.0f);
    private final Vector3f cameraFront = new Vector3f(1.0f, 0f, 0.0f);
    private final Vector3f cameraUp = new Vector3f(0.0f, 1.0f, 0.0f);
    private float cameraSpeed = 0.05f;

    private double lastX, lastY;
    private boolean firstMouse = true;
    // must be changed with change of cameraPos
    private float yaw = 0.0f;
    private float pitch = 0.0f;
    private final float mouseSensitivity = 0.2f;

    private final long windowID;

    private boolean leftMouseButtonPressed = false;

    public ControlsUtil(long windowID) {
        this.windowID = windowID;
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
            double dy = lastY - ypos; // Reversed since OpenGL y-coordinates go bottom to top

            lastX = xpos;
            lastY = ypos;

            dx *= mouseSensitivity;
            dy *= mouseSensitivity;

            yaw += (float) dx;
            pitch += (float) dy;

            // Limit pitch to prevent flipping
            if (pitch > 89.0f) pitch = 89.0f;
            if (pitch < -89.0f) pitch = -89.0f;

            updateCameraVectors();
        });
    }

    private void updateCameraVectors() {
        float x = (float) (Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
        float y = (float) Math.sin(Math.toRadians(pitch));
        float z = (float) (Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));

        cameraFront.set(x, y, z).normalize();
    }

    /**
     * Update view matrix in openGL state
     */
    public void updateViewMatrix(int viewLoc) {
        Matrix4f view = new Matrix4f();
        view.lookAt(cameraPos, cameraPos.add(cameraFront, new Vector3f()), cameraUp);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer viewBuffer = view.get(stack.mallocFloat(16));
            glUniformMatrix4fv(viewLoc, false, viewBuffer);
        }
    }

    /**
     * Handle user control of app
     */
    public void processInput() {
        if (glfwGetKey(windowID, GLFW_KEY_W) == GLFW_PRESS) {
            cameraPos.add(cameraFront.mul(cameraSpeed, new Vector3f()));
        }
        if (glfwGetKey(windowID, GLFW_KEY_S) == GLFW_PRESS) {
            cameraPos.sub(cameraFront.mul(cameraSpeed, new Vector3f()));
        }
        if (glfwGetKey(windowID, GLFW_KEY_A) == GLFW_PRESS) {
            Vector3f left = new Vector3f();
            cameraFront.cross(cameraUp, left).normalize().mul(cameraSpeed);
            cameraPos.sub(left);
        }
        if (glfwGetKey(windowID, GLFW_KEY_D) == GLFW_PRESS) {
            Vector3f right = new Vector3f();
            cameraFront.cross(cameraUp, right).normalize().mul(cameraSpeed);
            cameraPos.add(right);
        }
        if (glfwGetKey(windowID, GLFW_KEY_E) == GLFW_PRESS) {
            cameraPos.add(new Vector3f(cameraUp).mul(cameraSpeed));  // Move up
        }
        if (glfwGetKey(windowID, GLFW_KEY_Q) == GLFW_PRESS) {
            cameraPos.sub(new Vector3f(cameraUp).mul(cameraSpeed));  // Move down
        }
        if (glfwGetMouseButton(windowID, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS) {
            leftMouseButtonPressed = true;
        } else {
            firstMouse = true;
            leftMouseButtonPressed = false;
        }
    }
}
