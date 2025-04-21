package objects;

import static org.lwjgl.opengl.GL30.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.stb.STBImage;

public class Sphere {
    private int vao, vbo, ebo;
    private int indexCount;
    private float[] uniformColor = {1.0f, 1.0f, 1.0f};

    public Sphere(int stacks, int slices) {
        generateSphere(stacks, slices);
    }

    private void generateSphere(int stacks, int slices) {
        float radius = 1.0f;
        float[] vertices;
        int[] indices;

        // Number of vertices
        int vertexCount = (stacks + 1) * (slices + 1);
        int indexCount = stacks * slices * 6; // Two triangles per quad

        vertices = new float[vertexCount * 8]; // Each vertex has (x, y, z) + (r, g, b) + (t, z)
        indices = new int[indexCount];

        int v = 0, i = 0;

        // Generate vertices
        for (int stack = 0; stack <= stacks; stack++) {
            float phi = (float) (Math.PI * stack / stacks); // Latitude
            for (int slice = 0; slice <= slices; slice++) {
                float theta = (float) (2.0 * Math.PI * slice / slices); // Longitude

                float x = (float) (Math.sin(phi) * Math.cos(theta));
                float y = (float) Math.cos(phi);
                float z = (float) (Math.sin(phi) * Math.sin(theta));

                // Vertex position
                vertices[v++] = x * radius;
                vertices[v++] = y * radius;
                vertices[v++] = z * radius;

                // Color (simple gradient)
                vertices[v++] = (x + 1) / 2;
                vertices[v++] = (y + 1) / 2;
                vertices[v++] = (z + 1) / 2;

                // Texture coordinates
                vertices[v++] = 1 - (slice / (float)slices);
                vertices[v++] = stack / (float)stacks;
            }
        }

        // Generate indices for triangle strips
        for (int stack = 0; stack < stacks; stack++) {
            for (int slice = 0; slice < slices; slice++) {
                int first = (stack * (slices + 1)) + slice;
                int second = first + slices + 1;

                indices[i++] = first;
                indices[i++] = second;
                indices[i++] = first + 1;

                indices[i++] = second;
                indices[i++] = second + 1;
                indices[i++] = first + 1;
            }
        }

        this.indexCount = i;

        // Save data to OpenGL
        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        FloatBuffer vertexBuffer = MemoryUtil.memAllocFloat(vertices.length);
        vertexBuffer.put(vertices).flip();
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);
        MemoryUtil.memFree(vertexBuffer);

        ebo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        IntBuffer indexBuffer = MemoryUtil.memAllocInt(indices.length);
        indexBuffer.put(indices).flip();
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);
        MemoryUtil.memFree(indexBuffer);

        // Set vertex attributes
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 8 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        // Set indices attributes
        glVertexAttribPointer(1, 3, GL_FLOAT, false, 8 * Float.BYTES, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);
        // Set texture attributes
        glVertexAttribPointer(2, 2, GL_FLOAT, false, 8 * Float.BYTES, 6 * Float.BYTES);
        glEnableVertexAttribArray(2);

        glBindVertexArray(0);
    }

    public void setColor(float r, float g, float b) {
        uniformColor[0] = r;
        uniformColor[1] = g;
        uniformColor[2] = b;
    }

    public void render() {
        glBindVertexArray(vao);
        glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
    }

    public int loadTexture() {
        int textureID = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureID);

        // Set texture parameters
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        // Load image using STB
        IntBuffer width = BufferUtils.createIntBuffer(1);
        IntBuffer height = BufferUtils.createIntBuffer(1);
        IntBuffer channels = BufferUtils.createIntBuffer(1);

        ByteBuffer image = STBImage.stbi_load("res/earth.png", width, height, channels, 0);
        if (image != null) {
            int format = (channels.get(0) == 4) ? GL_RGBA : GL_RGB;
            glTexImage2D(GL_TEXTURE_2D, 0, format, width.get(0), height.get(0), 0, format, GL_UNSIGNED_BYTE, image);
            glGenerateMipmap(GL_TEXTURE_2D);
            STBImage.stbi_image_free(image);
        } else {
            System.err.println("Failed to load texture");
        }

        return textureID;
    }

    public void cleanup() {
        glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo);
        glDeleteBuffers(ebo);
    }
}
