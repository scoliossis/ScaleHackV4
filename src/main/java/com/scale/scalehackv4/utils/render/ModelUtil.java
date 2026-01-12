package com.scale.scalehackv4.utils.render;

import com.scale.scalehackv4.Main;
import lombok.AllArgsConstructor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

// straight ripped from another project i made a while ago not for minecraft
public class ModelUtil {
    @AllArgsConstructor
    public static class Model {
        public ModelFace[] faces;

        public Box box;
    }

    public record ModelFace(Triangle vertex, Triangle textureCoordinates, Triangle normals) {

    }

    public record Triangle(Vec3f vertex1, Vec3f vertex2, Vec3f vertex3) {
        public static final Triangle EMPTY = new Triangle(Vec3f.ZERO, Vec3f.ZERO, Vec3f.ZERO);
    }

    public static Model getModel(String obj) {
        try {
            Vec3d minBox = new Vec3d(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
            Vec3d maxBox = new Vec3d(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);

            ArrayList<Vec3f> vertices = new ArrayList<>();
            ArrayList<Vec3f> normals = new ArrayList<>();
            ArrayList<Vec3f> textureCoordinates = new ArrayList<>();

            ArrayList<ModelFace> faces = new ArrayList<>();

            InputStream fileStream = Main.class.getResourceAsStream(obj);
            assert fileStream != null;

            BufferedReader fileReader = new BufferedReader(new InputStreamReader(fileStream));

            for (String line; (line = fileReader.readLine()) != null;) {
                // some people format their objs with extra spaces ig.
                line = line.replaceAll("\\s+", " ");

                String[] lineSplit = line.split(" ");

                switch (lineSplit[0]) {
                    case "v" -> {
                        vertices.add(new Vec3f(
                                Float.parseFloat(lineSplit[1]),
                                Float.parseFloat(lineSplit[2]),
                                Float.parseFloat(lineSplit[3])
                        ));

                        minBox = new Vec3d(
                                Math.min(minBox.x, Float.parseFloat(lineSplit[1])),
                                Math.min(minBox.y, Float.parseFloat(lineSplit[2])),
                                Math.min(minBox.z, Float.parseFloat(lineSplit[3]))
                        );

                        maxBox = new Vec3d(
                                Math.max(maxBox.x, Float.parseFloat(lineSplit[1])),
                                Math.max(maxBox.y, Float.parseFloat(lineSplit[2])),
                                Math.max(maxBox.z, Float.parseFloat(lineSplit[3]))
                        );
                    }
                    case "vt" -> textureCoordinates.add(new Vec3f(
                            Float.parseFloat(lineSplit[1]),
                            Float.parseFloat(lineSplit[2]),
                            0
                    ));
                    case "vn" -> normals.add(new Vec3f(
                            Float.parseFloat(lineSplit[1]),
                            Float.parseFloat(lineSplit[2]),
                            Float.parseFloat(lineSplit[3])
                    ));
                    case "f" -> {
                        int amountOfValues = lineSplit.length-1;

                        for (int i = 0; i < amountOfValues-1; i+=2) {
                            String[] vectorParts = new String[] {
                                    lineSplit[i + 1],
                                    lineSplit[((i + 1) % amountOfValues) + 1],
                                    lineSplit[((i + 2) % amountOfValues) + 1],
                            };

                            Triangle vertexTriangle = new Triangle(
                                    vertices.get(getFaceElement(vectorParts[0], 0)),
                                    vertices.get(getFaceElement(vectorParts[1], 0)),
                                    vertices.get(getFaceElement(vectorParts[2], 0))
                            );

                            Triangle textureCoordinatesTriangle = textureCoordinates.isEmpty() ? Triangle.EMPTY : new Triangle(
                                    textureCoordinates.get(getFaceElement(vectorParts[0], 1)),
                                    textureCoordinates.get(getFaceElement(vectorParts[1], 1)),
                                    textureCoordinates.get(getFaceElement(vectorParts[2], 1))
                            );

                            Triangle normalsTriangle = normals.isEmpty() ? Triangle.EMPTY : new Triangle(
                                    normals.get(getFaceElement(vectorParts[0], 2)),
                                    normals.get(getFaceElement(vectorParts[1], 2)),
                                    normals.get(getFaceElement(vectorParts[2], 2))
                            );

                            faces.add(new ModelFace(vertexTriangle, textureCoordinatesTriangle, normalsTriangle));
                        }
                    }
                }
            }

            return new Model(faces.toArray(ModelFace[]::new), new Box(minBox, maxBox));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static int getFaceElement(String face, int elementInt) {
        return Integer.parseInt(face.split("/")[elementInt]) - 1;
    }
}
