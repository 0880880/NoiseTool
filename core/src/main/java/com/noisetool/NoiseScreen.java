package com.noisetool;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.XmlReader;
import com.badlogic.gdx.utils.XmlWriter;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import imgui.ImGui;
import imgui.extension.texteditor.TextEditor;
import imgui.extension.texteditor.TextEditorLanguageDefinition;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import imgui.type.ImBoolean;
import imgui.type.ImInt;
import lombok.AllArgsConstructor;
import make.some.noise.Noise;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.nfd.NativeFileDialog;
import space.earlygrey.shapedrawer.ShapeDrawer;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.StringWriter;

public class NoiseScreen implements Screen {

    public final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
    public final ImGuiImplGl3 imGuiGl3 = new ImGuiImplGl3();

    int[] resolution = new int[] {512};
    ImInt seed = new ImInt(MathUtils.random(Integer.MAX_VALUE - 1));

    Color[][] map;

    Noise generator = new Noise();

    Array<NoiseLayer> noiseLayers = new Array<>();

    SpriteBatch batch;
    ShapeDrawer drawer;
    ExtendViewport viewport;

    long generationSpeed = 0;

    boolean changed = false;
    ImBoolean autoGenerate = new ImBoolean();

    void generate() {

        long start = System.currentTimeMillis();
        generator.setSeed(seed.get());
        map = new Color[resolution[0]][resolution[0]];
        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map.length; j++) {
                map[i][j] = new Color(0,0,0,1);
                for (NoiseLayer layer : noiseLayers) {
                    generator.setFrequency(layer.frequency.get());
                    float val = 0;

                    if (layer.noiseType == NoiseType.Perlin) {
                        val = generator.getPerlin(i, j) * layer.amplitude.get();
                    } else if (layer.noiseType == NoiseType.PerlinFractal) {
                        generator.setFractalGain(layer.fractalGain.get());
                        generator.setFractalOctaves(layer.fractalOctaves.get());
                        val = generator.getPerlinFractal(i, j) * layer.amplitude.get();
                    } else if (layer.noiseType == NoiseType.Cubic) {
                        val = generator.getCubic(i, j) * layer.amplitude.get();
                    } else if (layer.noiseType == NoiseType.CubicFractal) {
                        generator.setFractalGain(layer.fractalGain.get());
                        generator.setFractalOctaves(layer.fractalOctaves.get());
                        val = generator.getCubicFractal(i, j) * layer.amplitude.get();
                    } else if (layer.noiseType == NoiseType.Foam) {
                        val = generator.getFoam(i, j) * layer.amplitude.get();
                    } else if (layer.noiseType == NoiseType.FoamFractal) {
                        generator.setFractalGain(layer.fractalGain.get());
                        generator.setFractalOctaves(layer.fractalOctaves.get());
                        val = generator.getFoamFractal(i, j) * layer.amplitude.get();
                    } else if (layer.noiseType == NoiseType.Honey) {
                        val = generator.getHoney(i, j) * layer.amplitude.get();
                    } else if (layer.noiseType == NoiseType.HoneyFractal) {
                        generator.setFractalGain(layer.fractalGain.get());
                        generator.setFractalOctaves(layer.fractalOctaves.get());
                        val = generator.getHoneyFractal(i, j) * layer.amplitude.get();
                    } else if (layer.noiseType == NoiseType.Cellular) {
                        val = generator.getCellular(i, j) * layer.amplitude.get();
                    } else if (layer.noiseType == NoiseType.Value) {
                        val = generator.getValue(i, j) * layer.amplitude.get();
                    } else if (layer.noiseType == NoiseType.ValueFractal) {
                        generator.setFractalGain(layer.fractalGain.get());
                        generator.setFractalOctaves(layer.fractalOctaves.get());
                        val = generator.getValueFractal(i, j) * layer.amplitude.get();
                    } else if (layer.noiseType == NoiseType.Simplex) {
                        val = generator.getSimplex(i, j) * layer.amplitude.get();
                    } else if (layer.noiseType == NoiseType.SimplexFractal) {
                        generator.setFractalGain(layer.fractalGain.get());
                        generator.setFractalOctaves(layer.fractalOctaves.get());
                        val = generator.getSimplexFractal(i, j) * layer.amplitude.get();
                    } else if (layer.noiseType == NoiseType.WhiteNoise) {
                        val = generator.getWhiteNoise(i, j) * layer.amplitude.get();
                    }

                    map[i][j].add(val, val, val, 0);
                }
            }
        }
        long end = System.currentTimeMillis();
        generationSpeed = end - start;

        StringBuilder code = new StringBuilder();

        code.append("int seed = ").append(seed.get()).append(";\n");
        code.append("int size = ").append(resolution[0]).append(";\n\n");

        int idx = 0;
        for (NoiseLayer layer : noiseLayers) {
            String name = "noise" + idx;
            code.append("Noise ").append(name).append(" = new Noise();\n");
            code.append(name).append(".setSeed(seed);\n");
            code.append(name).append(".setFrequency(").append(layer.frequency.get()).append("f);\n");
            if (isFractal(layer)) {
                code.append(name).append(".setFractalGain(").append(layer.fractalGain.get()).append("f);\n");
                code.append(name).append(".setFractalOctaves(").append(layer.fractalOctaves.get()).append(");\n");
            }
            code.append("\n");
            idx++;
        }

        code.append("for (i = 0; i < size; i++) {\n");
        code.append("   for (j = 0; j < size; j++) {\n");
        code.append("       ");
        code.append("float height = ");
        idx = 0;
        for (NoiseLayer layer : noiseLayers) {
            String name = "noise" + idx;
            if (layer.noiseType == NoiseType.Perlin) {
                code.append(name).append(".getPerlin(i, j)");
            } else if (layer.noiseType == NoiseType.PerlinFractal) {
                code.append(name).append(".getPerlinFractal(i, j)");
            } else if (layer.noiseType == NoiseType.Cubic) {
                code.append(name).append(".getCubic(i, j)");
            } else if (layer.noiseType == NoiseType.CubicFractal) {
                code.append(name).append(".getCubicFractal(i, j)");
            } else if (layer.noiseType == NoiseType.Foam) {
                code.append(name).append(".getFoam(i, j)");
            } else if (layer.noiseType == NoiseType.FoamFractal) {
                code.append(name).append(".getFoamFractal(i, j)");
            } else if (layer.noiseType == NoiseType.Honey) {
                code.append(name).append(".getHoney(i, j)");
            } else if (layer.noiseType == NoiseType.HoneyFractal) {
                code.append(name).append(".getHoneyFractal(i, j)");
            } else if (layer.noiseType == NoiseType.Cellular) {
                code.append(name).append(".getCellular(i, j)");
            } else if (layer.noiseType == NoiseType.Value) {
                code.append(name).append(".getValue(i, j)");
            } else if (layer.noiseType == NoiseType.ValueFractal) {
                code.append(name).append(".getValueFractal(i, j)");
            } else if (layer.noiseType == NoiseType.Simplex) {
                code.append(name).append(".getSimplex(i, j)");
            } else if (layer.noiseType == NoiseType.SimplexFractal) {
                code.append(name).append(".getSimplexFractal(i, j)");
            } else if (layer.noiseType == NoiseType.WhiteNoise) {
                code.append(name).append(".getWhiteNoise(i, j)");
            }
            if (idx < noiseLayers.size - 1) code.append(" + ");
            idx++;
        }
        code.append(";\n");
        code.append("   }\n");
        code.append("}\n");

        textEditor.setText(code.toString());

    }

    TextEditor textEditor;

    @Override
    public void show() {

        long handle = ((Lwjgl3Graphics) Gdx.graphics).getWindow().getWindowHandle();
        GLFW.glfwMakeContextCurrent(handle);
        ImGui.setCurrentContext(ImGui.createContext());

        imGuiGlfw.init(handle, true);
        imGuiGl3.init("#version 330");

        textEditor = new TextEditor();
        textEditor.setReadOnly(true);
        textEditor.setTabSize(4);
        textEditor.setShowWhitespaces(false);
        textEditor.setLanguageDefinition(TextEditorLanguageDefinition.cPlusPlus());

        batch = new SpriteBatch();

        Pixmap pixel = new Pixmap(1,1,Pixmap.Format.RGBA8888);
        pixel.setColor(Color.WHITE);
        pixel.fill();

        drawer = new ShapeDrawer(batch, new TextureRegion(new Texture(pixel)));

        viewport = new ExtendViewport(1920,1080);

        noiseLayers.add(new NoiseLayer());

        generate();

    }

    float scl = 2;
    float timer = 0;

    void save() {
        PointerBuffer pointer = BufferUtils.createPointerBuffer(1);
        NativeFileDialog.NFD_SaveDialog("nt", System.getProperty("user.home"), pointer);
        long addr = pointer.get();

        if (addr != 0) {
            String location = MemoryUtil.memUTF8(addr);
            generate();
            StringWriter writer = new StringWriter();
            try {
                XmlWriter xml = new XmlWriter(writer).element("main");
                xml.attribute("seed", seed);
                xml.attribute("resolution", resolution[0]);
                for (NoiseLayer layer : noiseLayers) {
                    xml.element("layer")
                            .attribute("type", layer.noiseType)
                            .attribute("frequency", layer.frequency.get())
                            .attribute("amplitude", layer.amplitude.get())
                            .attribute("fractalGain", layer.fractalGain.get())
                            .attribute("fractalOctaves", layer.fractalOctaves.get())
                            .pop();
                }
                xml.pop();
                xml.close();
                Gdx.files.absolute(location).writeString(writer.toString(), false);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    void open() {

        PointerBuffer pointer = BufferUtils.createPointerBuffer(1);
        NativeFileDialog.NFD_OpenDialog("nt", System.getProperty("user.home"), pointer);
        long addr = pointer.get();

        if (addr != 0) {
            noiseLayers.clear();
            XmlReader xml = new XmlReader();
            XmlReader.Element main = xml.parse(Gdx.files.absolute(MemoryUtil.memUTF8(addr)));
            seed.set(Integer.parseInt(main.getAttribute("seed")));
            resolution[0] = Integer.parseInt(main.getAttribute("resolution"));
            for (XmlReader.Element layer : main.getChildrenByName("layer")) {
                NoiseLayer nl = new NoiseLayer();
                nl.noiseType = NoiseType.valueOf(layer.getAttribute("type"));
                nl.amplitude.set(Float.parseFloat(layer.getAttribute("amplitude")));
                nl.frequency.set(Float.parseFloat(layer.getAttribute("frequency")));
                nl.amplitude.set(Float.parseFloat(layer.getAttribute("amplitude")));
                nl.fractalGain.set(Float.parseFloat(layer.getAttribute("fractalGain")));
                nl.fractalOctaves.set(Integer.parseInt(layer.getAttribute("fractalOctaves")));
                noiseLayers.add(nl);
            }
        }
        generate();
    }

    void copy() {
        generate();
        BufferedImage image = new BufferedImage(resolution[0], resolution[0], BufferedImage.TYPE_INT_RGB);
        for (int i = 0; i < resolution[0]; i++) {
            for (int j = 0; j < resolution[0]; j++) {
                image.setRGB(i, j, Color.rgb888(map[i][j]));
            }
        }

        ImageTransferable transferable = new ImageTransferable(image);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(transferable, null);
    }

    boolean isFractal(NoiseLayer layer) {
        return layer.noiseType == NoiseType.PerlinFractal || layer.noiseType == NoiseType.FoamFractal || layer.noiseType == NoiseType.CubicFractal || layer.noiseType == NoiseType.HoneyFractal || layer.noiseType == NoiseType.ValueFractal;
    }

    boolean showCode = false;

    @Override
    public void render(float delta) {

        Gdx.graphics.setTitle("NoiseTool" + (changed ? "*" : "") + " : FPS: " + Gdx.graphics.getFramesPerSecond());

        ScreenUtils.clear(1,0,1,1);

        viewport.apply();

        timer += delta;

        if (autoGenerate.get() && timer >= .4f) {
            timer = 0;
            generate();
        }

        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();

        scl = 4f / (map.length / 256f);

        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map.length; j++) {
                drawer.filledRectangle(i * scl - (map.length / 2f * scl), j * scl - (map.length / 2f * scl), scl, scl, map[i][j]);
            }
        }

        if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) && Gdx.input.isKeyJustPressed(Input.Keys.C))
            copy();
        if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) && Gdx.input.isKeyJustPressed(Input.Keys.S))
            save();
        if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) && Gdx.input.isKeyJustPressed(Input.Keys.O))
            open();

        batch.end();

        imGuiGlfw.newFrame();
        ImGui.newFrame();

        ImGui.beginMainMenuBar();

        if (ImGui.beginMenu("File")) {

            if (ImGui.menuItem("New                        ")) {
                noiseLayers.clear();
                noiseLayers.add(new NoiseLayer());
                seed = new ImInt(MathUtils.random(Integer.MAX_VALUE - 1));
            }
            if (ImGui.menuItem("Save", "Ctrl + S")) {
                save();
            }
            if (ImGui.menuItem("Open", "Ctrl + O")) {
                open();
            }
            if (ImGui.menuItem("Copy Image", "Ctrl + C")) copy();
            if (ImGui.menuItem("Exit")) Gdx.app.exit();

            ImGui.endMenu();
        }

        ImGui.endMainMenuBar();

        ImGui.begin("Settings");

        ImGui.sliderInt("Map resolution", resolution, 128, 1024);
        ImGui.inputInt("Seed", seed);

        ImGui.separator();

        ImGui.text("Layers");
        ImGui.spacing();

        for (int i = 0; i < noiseLayers.size; i++) {
            NoiseLayer layer = noiseLayers.get(i);

            if (ImGui.treeNode("Layer #" + i)) {
                if (ImGui.beginPopupContextItem("LayerMenu")) {

                    if (ImGui.selectable("Remove"))
                        noiseLayers.removeIndex(i);

                    ImGui.endPopup();
                }
                ImGui.indent();

                if (ImGui.beginCombo("Type", layer.noiseType.toString())) {
                    if (ImGui.selectable("Value")) layer.noiseType = NoiseType.Value;
                    if (ImGui.selectable("ValueFractal")) layer.noiseType = NoiseType.ValueFractal;
                    if (ImGui.selectable("Perlin")) layer.noiseType = NoiseType.Perlin;
                    if (ImGui.selectable("PerlinFractal")) layer.noiseType = NoiseType.PerlinFractal;
                    if (ImGui.selectable("Cubic")) layer.noiseType = NoiseType.Cubic;
                    if (ImGui.selectable("CubicFractal")) layer.noiseType = NoiseType.CubicFractal;
                    if (ImGui.selectable("Foam")) layer.noiseType = NoiseType.Foam;
                    if (ImGui.selectable("FoamFractal")) layer.noiseType = NoiseType.FoamFractal;
                    if (ImGui.selectable("Honey")) layer.noiseType = NoiseType.Honey;
                    if (ImGui.selectable("HoneyFractal")) layer.noiseType = NoiseType.HoneyFractal;
                    if (ImGui.selectable("Cellular")) layer.noiseType = NoiseType.Cellular;

                    ImGui.endCombo();
                }

                ImGui.inputFloat("Frequency", layer.frequency);
                ImGui.inputFloat("Amplitude", layer.amplitude);

                if (isFractal(layer)) {
                    ImGui.inputFloat("Fractal Gain", layer.fractalGain);
                    ImGui.inputInt("Fractal Octaves", layer.fractalOctaves);
                }

                ImGui.unindent();
                ImGui.treePop();
            } else {
                if (ImGui.beginPopupContextItem("LayerMenu")) {

                    if (ImGui.selectable("Remove"))
                        noiseLayers.removeIndex(i);

                    ImGui.endPopup();
                }
            }
        }

        if (ImGui.button("Add Layer"))
            noiseLayers.add(new NoiseLayer(.5f, .5f));

        ImGui.separator();

        ImGui.checkbox("Auto generate", autoGenerate);

        if (ImGui.button("Generate"))
            generate();
        ImGui.sameLine();
        if (ImGui.button("Show Code"))
            showCode = !showCode;

        ImGui.text("" + generationSpeed + "ms");

        ImGui.end();

        if (showCode) {
            ImGui.setNextWindowSizeConstraints(300,160,10000,10000);
            ImGui.begin("Code");

            textEditor.render("Code");

            ImGui.end();
        }

        ImGui.render();
        imGuiGl3.renderDrawData(ImGui.getDrawData());

    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
    }

    @Override
    public void pause() { }

    @Override
    public void resume() { }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() { }

    @AllArgsConstructor
    static class ImageTransferable implements Transferable {
        private Image image;

        public Object getTransferData(DataFlavor flavor)
                throws UnsupportedFlavorException {
            if (isDataFlavorSupported(flavor)) {
                return image;
            } else {
                throw new UnsupportedFlavorException(flavor);
            }
        }

        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return flavor == DataFlavor.imageFlavor;
        }

        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{DataFlavor.imageFlavor};
        }
    }

}