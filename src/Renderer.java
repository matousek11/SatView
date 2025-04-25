import objects.Sphere;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;
import shaders.BaseFragmentShader;
import shaders.BaseVertexShader;
import shaders.EarthFragmentShader;
import shaders.EarthVertexShader;
import utils.ControlsUtil;
import utils.DataProvider;
import utils.SimpleTextRenderer;

import java.nio.FloatBuffer;
import java.util.ArrayList;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.system.MemoryStack.stackPush;

public class Renderer {
    private final int windowWidth;
    private final int windowHeight;
    private final int earthRadius;
    private final ControlsUtil controlsUtil;
    private final DataProvider dataProvider;
    private final ArrayList<Integer> satelliteIDs;
    private final SimpleTextRenderer timeRenderer;
    private final Sphere satelliteSphere;
    private final Sphere earthSphere;
    private final SatellitePicker satellitePicker;

    private int baseShaderID;
    private int earthShaderID;
    private int earthTextureID;

    public Renderer(int windowWidth, int windowHeight, int earthRadius, ControlsUtil controlsUtil,
                   DataProvider dataProvider, ArrayList<Integer> satelliteIDs) {
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;
        this.earthRadius = earthRadius;
        this.controlsUtil = controlsUtil;
        this.dataProvider = dataProvider;
        this.satelliteIDs = satelliteIDs;

        // Initialize renderers and objects
        this.timeRenderer = new SimpleTextRenderer(windowWidth, windowHeight, 24);
        this.satelliteSphere = new Sphere(8, 8);
        this.earthSphere = new Sphere(40, 40);
        this.satellitePicker = new SatellitePicker(windowWidth, windowHeight, dataProvider, satelliteIDs, earthRadius);

        // Initialize shaders
        initializeShaders();
    }

    private void initializeShaders() {
        // Base shader for satellites
        baseShaderID = glCreateProgram();
        BaseVertexShader baseVertexShader = new BaseVertexShader();
        baseVertexShader.setup(baseShaderID);
        BaseFragmentShader baseFragmentShader = new BaseFragmentShader();
        baseFragmentShader.setup(baseShaderID);
        glLinkProgram(baseShaderID);
        baseVertexShader.deleteShader();
        baseFragmentShader.deleteShader();

        // Earth shader
        earthShaderID = glCreateProgram();
        EarthVertexShader earthVertexShader = new EarthVertexShader();
        earthVertexShader.setup(earthShaderID);
        EarthFragmentShader earthFragmentShader = new EarthFragmentShader();
        earthFragmentShader.setup(earthShaderID);
        glLinkProgram(earthShaderID);
        earthVertexShader.deleteShader();
        earthFragmentShader.deleteShader();

        // Load earth texture
        earthTextureID = earthSphere.loadTexture();
    }

    public void render(double satelliteTime, String timeString) {
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LESS);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        renderSatellites(satelliteTime);
        renderEarth();
        timeRenderer.setText(timeString);
        renderText();
    }

    private void renderSatellites(double satelliteTime) {
        glUseProgram(baseShaderID);
        controlsUtil.updateViewMatrix(glGetUniformLocation(baseShaderID, "view"));

        // Define colors for satellites
        float[] defaultColor = {0.8f, 1f, 0.8f}; // White color for all satellites
        float[] starlinkColor = {0f, 0.3f, 0.4f}; // Dark blue for starlink satellites
        float[] selectedColor = {0.8f, 0f, 0f}; // Red color for selected satellite

        for (Integer satelliteID : satelliteIDs) {
            float[] data = dataProvider.getRecord(satelliteID, (int)satelliteTime);
            if (data.length == 0) continue;

            float longitude = data[0];
            float latitude = data[1];
            float height = data[2];

            float[] color;
            if (satelliteID == satellitePicker.getSelectedSatelliteID()) {
                color = selectedColor.clone();
            } else if (dataProvider.getSatelliteNames().get(satelliteID) != null && 
                      dataProvider.getSatelliteNames().get(satelliteID).contains("STARLINK")) {
                color = starlinkColor.clone();
            } else {
                color = defaultColor.clone();
            }

            renderSatellite(longitude, latitude, height, color);
        }
    }

    private void renderSatellite(float longitude, float latitude, float height, float[] color) {
        try (MemoryStack stack = stackPush()) {
            Matrix4f model = new Matrix4f();
            model.identity();
            model.rotate((float) Math.toRadians(longitude), 0, 1, 0);
            model.rotate(-(float) Math.toRadians(latitude), 0, 0, 1);
            model.translate(new Vector3f(((-1 / (float) earthRadius) * (earthRadius + height)), 0, 0));
            model.scale(0.01f);

            Matrix4f projection = new Matrix4f();
            projection.setPerspective((float) Math.toRadians(45.0), (float) windowWidth / windowHeight, 0.1f, 100.0f);

            FloatBuffer modelBuffer = model.get(stack.mallocFloat(16));
            FloatBuffer projBuffer = projection.get(stack.mallocFloat(16));

            glUniformMatrix4fv(glGetUniformLocation(baseShaderID, "model"), false, modelBuffer);
            glUniformMatrix4fv(glGetUniformLocation(baseShaderID, "projection"), false, projBuffer);
            glUniform3f(glGetUniformLocation(baseShaderID, "color"), color[0], color[1], color[2]);
        }

        satelliteSphere.render();
    }

    private void renderEarth() {
        glUseProgram(earthShaderID);
        
        try (MemoryStack stack = stackPush()) {
            Matrix4f model = new Matrix4f();
            model.identity();
            Matrix4f projection = new Matrix4f();
            projection.setPerspective((float) Math.toRadians(45.0), (float) windowWidth / windowHeight, 0.1f, 100.0f);

            FloatBuffer modelBuffer = model.get(stack.mallocFloat(16));
            FloatBuffer projBuffer = projection.get(stack.mallocFloat(16));

            glUniformMatrix4fv(glGetUniformLocation(earthShaderID, "model"), false, modelBuffer);
            glUniformMatrix4fv(glGetUniformLocation(earthShaderID, "projection"), false, projBuffer);
        }

        glUniform1i(glGetUniformLocation(earthShaderID, "earthTexture"), 0);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, earthTextureID);
        controlsUtil.updateViewMatrix(glGetUniformLocation(earthShaderID, "view"));

        earthSphere.render();
    }

    private void renderText() {
        glClear(GL_DEPTH_BUFFER_BIT);
        timeRenderer.drawText(10, 20);

        if (satellitePicker.getSelectedSatelliteID() != -1) {
            String satelliteName = dataProvider.getSatelliteNames().get(satellitePicker.getSelectedSatelliteID());
            String selectedInfo = "Name: " + satelliteName.split(";")[0];
            SimpleTextRenderer satelliteInfo = new SimpleTextRenderer(windowWidth, windowHeight, 18);
            satelliteInfo.setText(selectedInfo);
            satelliteInfo.drawText(10, 50);
            
            SimpleTextRenderer noradID = new SimpleTextRenderer(windowWidth, windowHeight, 18);
            noradID.setText("NORAD ID: " + satelliteName.split(";")[1]);
            noradID.drawText(10, 70);
        }

        if (controlsUtil.getShowInfobox()) {
            SimpleTextRenderer infoText = new SimpleTextRenderer(windowWidth, windowHeight, 18);
            infoText.setText("Vizualizace polohy satelitu | Lukas Matousek | PGRF2");
            infoText.drawText(10, windowHeight - 15);
            
            SimpleTextRenderer controlText = new SimpleTextRenderer(windowWidth, windowHeight, 18);
            controlText.setText("i - infobox, j - time back, k - time forward, n - normal speed, p - stop");
            controlText.drawText(10, windowHeight - 30);
        }
    }

    public void cleanup() {
        earthSphere.cleanup();
        satelliteSphere.cleanup();
        timeRenderer.cleanup();
        glDeleteProgram(baseShaderID);
        glDeleteProgram(earthShaderID);
        satellitePicker.cleanup();
    }

    public SatellitePicker getSatellitePicker() {
        return satellitePicker;
    }
} 