package utils;

import org.lwjgl.BufferUtils;
import org.lwjgl.stb.*;
import org.lwjgl.system.MemoryStack;
import shaders.TextVertexShader;
import shaders.TextFragmentShader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class SimpleTextRenderer {
    private static final int FIRST_CHAR = 32;
    private static final int CHAR_COUNT = 96;
    private static final int ATLAS_W = 1024, ATLAS_H = 1024;
    private static final float SCALE = 1.0f;

    private final int textureID;
    private final STBTTBakedChar.Buffer charData;
    private final int vao, vbo;
    private final int shaderProgram;
    private final FloatBuffer vertices;
    private final int screenWidth, screenHeight;
    private String currentText;

    public SimpleTextRenderer(int screenWidth, int screenHeight, int fontSize) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.currentText = "";

        try {
            // 1) Load TTF into ByteBuffer
            ByteBuffer ttf = ioResourceToByteBuffer("res/arial.ttf");

            // 2) Create and compile shaders
            shaderProgram = glCreateProgram();
            TextVertexShader vertexShader = new TextVertexShader();
            TextFragmentShader fragmentShader = new TextFragmentShader();
            vertexShader.setup(shaderProgram);
            fragmentShader.setup(shaderProgram);
            glLinkProgram(shaderProgram);

            if (glGetProgrami(shaderProgram, GL_LINK_STATUS) == GL_FALSE) {
                String log = glGetProgramInfoLog(shaderProgram);
                throw new RuntimeException("Error linking shader program: " + log);
            }

            vertexShader.deleteShader();
            fragmentShader.deleteShader();

            // 3) Bake font bitmap
            textureID = glGenTextures();
            charData = STBTTBakedChar.malloc(CHAR_COUNT);

            ByteBuffer bitmap = BufferUtils.createByteBuffer(ATLAS_W * ATLAS_H);
            STBTruetype.stbtt_BakeFontBitmap(ttf, (float) fontSize, bitmap, ATLAS_W, ATLAS_H, FIRST_CHAR, charData);

            // 4) Upload as GL texture
            glBindTexture(GL_TEXTURE_2D, textureID);
            glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RED, ATLAS_W, ATLAS_H, 0, GL_RED, GL_UNSIGNED_BYTE, bitmap);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

            // 5) Setup VAO/VBO for quads
            vertices = BufferUtils.createFloatBuffer(6 * 4); // 6 vertices * 4 components (pos + uv)

            vao = glGenVertexArrays();
            vbo = glGenBuffers();

            glBindVertexArray(vao);
            glBindBuffer(GL_ARRAY_BUFFER, vbo);
            glBufferData(GL_ARRAY_BUFFER, vertices.capacity() * Float.BYTES, GL_DYNAMIC_DRAW);

            // Position attribute
            glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0);
            glEnableVertexAttribArray(0);
            // Texture coord attribute
            glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);
            glEnableVertexAttribArray(1);

            glBindVertexArray(0);
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize text renderer", e);
        }
    }

    public void setText(String text) {
        this.currentText = text;
    }

    public void drawText(int x, int y) {
        glUseProgram(shaderProgram);
        glUniform2f(glGetUniformLocation(shaderProgram, "uScreenSize"), screenWidth, screenHeight);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, textureID);
        glUniform1i(glGetUniformLocation(shaderProgram, "textAtlas"), 0);

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_DEPTH_TEST);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            STBTTAlignedQuad q = STBTTAlignedQuad.mallocStack(stack);
            FloatBuffer xBuf = stack.floats(x);
            FloatBuffer yBuf = stack.floats(y);

            glBindVertexArray(vao);

            for (int i = 0; i < currentText.length(); i++) {
                char c = currentText.charAt(i);
                if (c < FIRST_CHAR || c >= FIRST_CHAR + CHAR_COUNT) continue;

                STBTruetype.stbtt_GetBakedQuad(charData, ATLAS_W, ATLAS_H, c - FIRST_CHAR, xBuf, yBuf, q, true);

                float x0 = q.x0() * SCALE;
                float x1 = q.x1() * SCALE;
                float y0 = q.y0() * SCALE;
                float y1 = q.y1() * SCALE;

                // First triangle
                vertices.put(x0).put(y0).put(q.s0()).put(q.t0());
                vertices.put(x1).put(y0).put(q.s1()).put(q.t0());
                vertices.put(x0).put(y1).put(q.s0()).put(q.t1());

                // Second triangle
                vertices.put(x1).put(y0).put(q.s1()).put(q.t0());
                vertices.put(x1).put(y1).put(q.s1()).put(q.t1());
                vertices.put(x0).put(y1).put(q.s0()).put(q.t1());

                vertices.flip();

                glBindBuffer(GL_ARRAY_BUFFER, vbo);
                glBufferSubData(GL_ARRAY_BUFFER, 0, vertices);
                glDrawArrays(GL_TRIANGLES, 0, 6);

                vertices.clear();
            }

            glBindVertexArray(0);
        }

        glEnable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);
        glUseProgram(0);
    }

    private static ByteBuffer ioResourceToByteBuffer(String resource) throws IOException {
        try (FileChannel fc = FileChannel.open(Paths.get(resource), StandardOpenOption.READ)) {
            ByteBuffer bb = BufferUtils.createByteBuffer((int)fc.size() + 1);
            while (fc.read(bb) != -1);
            bb.flip();
            return bb;
        }
    }

    public void cleanup() {
        glDeleteTextures(textureID);
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
        glDeleteProgram(shaderProgram);
        charData.free();
    }
}
