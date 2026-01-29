package com.scale.scalehackv4.utils.client;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.awt.*;
import java.io.IOException;

// this is SO chud. java 8 (java 15 and below) gson parses the colour correctly :(
public class ColourAdapter extends TypeAdapter<Color> {
    @Override
    public void write(JsonWriter out, Color value) throws IOException {
        out.beginObject();
        out.name("rgb").value(value.getRGB());
        out.name("a").value(value.getAlpha());
        out.endObject();
    }

    @Override
    public Color read(JsonReader in) throws IOException {
        Color rgb = new Color(in.nextInt());
        int alpha = in.nextInt();
        return new Color(rgb.getRed(), rgb.getGreen(), rgb.getBlue(), alpha);
    }
}
