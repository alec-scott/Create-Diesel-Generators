package com.jesz.createdieselgenerators.client.render.gui;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

/**
 * Picture-in-picture state of a JEI machine drawing. {@code ux}, {@code uy} are the coordinates upstream passed to
 * {@code AnimatedKinetics.draw(graphics, x, y)}: upstream rotated the GUI pose about the corner of the first block,
 * which puts the bottom centre of that block at (ux + 15, uy + 1.66); the picture-in-picture box is placed so that this
 * point is the origin of {@link MachineRenderer}'s frame. The box is {@link #WIDTH} wide and reaches {@code up} pixels above
 * and {@code below} pixels under {@code uy}.
 */
public interface MachineState extends PictureInPictureRenderState {
    int WIDTH = 72;

    /** Upstream's block scale (px per block) in every machine drawing. */
    float BLOCK_SCALE = 23;

    /** Where the origin sits from the vertical centre of the first block's bottom: 22.5 degrees about Y, -15.5 about X. */
    float ORIGIN_DROP = 1.66f;

    static ScreenRectangle box(Matrix3x2f pose, int ux, int uy, int up, int below) {
        return new ScreenRectangle(x0(ux), y0(uy, up), WIDTH, up + below).transformMaxBounds(pose);
    }

    static int x0(int ux) {
        return ux + 15 - WIDTH / 2;
    }

    static int y0(int uy, int up) {
        return uy - up;
    }

    static int y1(int uy, int below) {
        return uy + below;
    }

    @Override
    default float scale() {
        return BLOCK_SCALE;
    }

    @Override
    default @Nullable ScreenRectangle scissorArea() {
        return null;
    }
}
