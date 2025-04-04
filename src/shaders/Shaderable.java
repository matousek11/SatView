package shaders;

public interface Shaderable {
    public void setup(int shaderProgram);
    public void deleteShader();
    public String getSource();
}
