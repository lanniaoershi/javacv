/*
 * Copyright (C) 2015 Samuel Audet
 *
 * This file is part of JavaCV.
 *
 * JavaCV is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * JavaCV is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JavaCV.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.bytedeco.javacv;

import java.nio.Buffer;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Pointer;

import static org.bytedeco.javacpp.opencv_core.*;

/**
 * A utility class to map data between {@link Frame} and {@link IplImage} or {@link Mat}.
 * Since this is an abstract class, one must choose between two concrete classes:
 * {@link ToIplImage} or {@link ToMat}.
 *
 * @author Samuel Audet
 */
public abstract class OpenCVFrameConverter<F> extends FrameConverter<F> {
    IplImage img;
    Mat mat;

    public static class ToIplImage extends OpenCVFrameConverter<IplImage> {
        @Override public Frame convert(IplImage img) { return super.convert(img); }
        @Override public IplImage convert(Frame frame) { return convertToIplImage(frame); }
    }

    public static class ToMat extends OpenCVFrameConverter<Mat> {
        @Override public Frame convert(Mat mat) { return super.convert(mat); }
        @Override public Mat convert(Frame frame) { return convertToMat(frame); }
    }

    public static int getFrameDepth(int depth) {
        switch (depth) {
            case IPL_DEPTH_8U:  case CV_8U:  return Frame.DEPTH_UBYTE;
            case IPL_DEPTH_8S:  case CV_8S:  return Frame.DEPTH_BYTE;
            case IPL_DEPTH_16U: case CV_16U: return Frame.DEPTH_USHORT;
            case IPL_DEPTH_16S: case CV_16S: return Frame.DEPTH_SHORT;
            case IPL_DEPTH_32F: case CV_32F: return Frame.DEPTH_FLOAT;
            case IPL_DEPTH_32S: case CV_32S: return Frame.DEPTH_INT;
            case IPL_DEPTH_64F: case CV_64F: return Frame.DEPTH_DOUBLE;
            default: return -1;
        }
    }

    public static int getIplImageDepth(Frame frame) {
        switch (frame.imageDepth) {
            case Frame.DEPTH_UBYTE:  return IPL_DEPTH_8U;
            case Frame.DEPTH_BYTE:   return IPL_DEPTH_8S;
            case Frame.DEPTH_USHORT: return IPL_DEPTH_16U;
            case Frame.DEPTH_SHORT:  return IPL_DEPTH_16S;
            case Frame.DEPTH_FLOAT:  return IPL_DEPTH_32F;
            case Frame.DEPTH_INT:    return IPL_DEPTH_32S;
            case Frame.DEPTH_DOUBLE: return IPL_DEPTH_64F;
            default:  return -1;
        }
    }
    static boolean isEqual(Frame frame, IplImage img) {
        return img != null && frame != null && frame.image != null && frame.image.length > 0
                && frame.imageWidth == img.width() && frame.imageHeight == img.height()
                && frame.imageChannels == img.nChannels() && getIplImageDepth(frame) == img.depth()
                && new Pointer(frame.image[0]).address() == img.imageData().address()
                && frame.imageStride * Math.abs(frame.imageDepth) / 8 == img.widthStep();
    }
    public IplImage convertToIplImage(Frame frame) {
        if (frame == null) {
            return null;
        } else if (frame.opaque instanceof IplImage) {
            return (IplImage)frame.opaque;
        } else if (!isEqual(frame, img)) {
            int depth = getIplImageDepth(frame);
            img = depth < 0 ? null : IplImage.createHeader(frame.imageWidth, frame.imageHeight, depth, frame.imageChannels)
                    .imageData(new BytePointer(new Pointer(frame.image[0].position(0)))).widthStep(frame.imageStride * Math.abs(frame.imageDepth) / 8);
        }
        return img;
    }
    public Frame convert(IplImage img) {
        if (img == null) {
            return null;
        } else if (!isEqual(frame, img)) {
            frame = new Frame();
            frame.imageWidth = img.width();
            frame.imageHeight = img.height();
            frame.imageDepth = getFrameDepth(img.depth());
            frame.imageChannels = img.nChannels();
            frame.imageStride = img.widthStep() * 8 / Math.abs(frame.imageDepth);
            frame.image = new Buffer[] { img.createBuffer() };
            frame.opaque = img;
        }
        return frame;
    }

    public static int getMatDepth(Frame frame) {
        switch (frame.imageDepth) {
            case Frame.DEPTH_UBYTE:  return CV_8U;
            case Frame.DEPTH_BYTE:   return CV_8S;
            case Frame.DEPTH_USHORT: return CV_16U;
            case Frame.DEPTH_SHORT:  return CV_16S;
            case Frame.DEPTH_FLOAT:  return CV_32F;
            case Frame.DEPTH_INT:    return CV_32S;
            case Frame.DEPTH_DOUBLE: return CV_64F;
            default:  return -1;
        }
    }
    static boolean isEqual(Frame frame, Mat mat) {
        return mat != null && frame != null && frame.image != null && frame.image.length > 0
                && frame.imageWidth == mat.cols() && frame.imageHeight == mat.rows()
                && frame.imageChannels == mat.channels() && getMatDepth(frame) == mat.depth()
                && new Pointer(frame.image[0]).address() == mat.data().address()
                && frame.imageStride * Math.abs(frame.imageDepth) / 8 == (int)mat.step();
    }
    public Mat convertToMat(Frame frame) {
        if (frame == null) {
            return null;
        } else if (frame.opaque instanceof Mat) {
            return (Mat)frame.opaque;
        } else if (!isEqual(frame, mat)) {
            int depth = getMatDepth(frame);
            mat = depth < 0 ? null : new Mat(frame.imageHeight, frame.imageWidth, CV_MAKETYPE(depth, frame.imageChannels),
                    new Pointer(frame.image[0].position(0)), frame.imageStride * Math.abs(frame.imageDepth) / 8);
        }
        return mat;
    }
    public Frame convert(Mat mat) {
        if (mat == null) {
            return null;
        } else if (!isEqual(frame, mat)) {
            frame = new Frame();
            frame.imageWidth = mat.cols();
            frame.imageHeight = mat.rows();
            frame.imageDepth = getFrameDepth(mat.depth());
            frame.imageChannels = mat.channels();
            frame.imageStride = (int)mat.step() * 8 / Math.abs(frame.imageDepth);
            frame.image = new Buffer[] { mat.createBuffer() };
            frame.opaque = mat;
        }
        return frame;
    }
}
