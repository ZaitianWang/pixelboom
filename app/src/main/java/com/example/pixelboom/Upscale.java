package com.example.pixelboom;

import org.bytedeco.opencv.opencv_core.Mat;
ximport org.opencv.core.Mat;
import org.bytedeco.opencv.opencv_dnn_superres.DnnSuperResImpl;
import org.opencv.dnn.*;
import org.opencv.android.Utils;

import static org.bytedeco.opencv.global.opencv_imgcodecs.*;

import android.graphics.Bitmap;

import java.io.File;

public class Upscale {
    private Upscale(){}

    public static boolean run(Bitmap bm, String savePath) {
        String[] mode = {"ESPCNx2", "espcn", "2"};
//        MainApp.write("Started Upscale Process ["+mode[0]+"]", null);

        //no savePath option
//        if(savePath == null) {
//            StringBuilder sb = new StringBuilder(loadPath);
//            savePath = sb.insert(sb.lastIndexOf("."),"("+mode[0]+")").toString();
//        }

//        MainApp.write("Loading Image",null);

//        Mat mat = new Mat();
//        Bitmap bmp32 = bmp.copy(Bitmap.Config.ARGB_8888, true);
//        Utils.bitmapToMat(bmp32, mat);

        Mat image = new Mat();
        Utils.bitmapToMat(bm, image);
        if (image.empty()) {
//            MainApp.write("Error Loading Image",MainApp.SCARLET);
            return false;
        }
        String modelName = "Models/"+mode[0]+".pb";
        Mat imageNew = new Mat();
//        MainApp.write("Loading AI",null);
        DnnSuperResImpl sr = null;
        try {
            sr = new DnnSuperResImpl();
            File modelPath = new File(new File(Upscale.class.getProtectionDomain().getCodeSource().getLocation()
                    .toURI().getPath()).getParent()+File.separator+modelName);
            if (!modelPath.exists()) {
//                MainApp.write("Model not found!",MainApp.SCARLET);
                return false;
            }
//            MainApp.write("Trying to read model from "+modelPath,null);
            sr.readModel(modelPath.toString());
            sr.setModel(mode[1], Integer.parseInt(mode[2]));
//            MainApp.write("Algorithm and Size Checked"+"\n \t Starting conversion",null);
            sr.upsample(image, imageNew);

//            MainApp.write("Image was successfully upScaled from "+originalSize+"x"+mode[2]+", to "+newSize+"and saved to:",null);
//            MainApp.write(savePath,MainApp.SVGBLUE);
//            Config.FIELD02.setValue(mode[0]);
            imwrite(savePath, imageNew);
            return true;
        } catch(Exception e) {
//            MainApp.write("Error UpScaling !",MainApp.SCARLET);
            e.printStackTrace();
            return false;
        }
        finally {
            imageNew.close();
            sr.deallocate();
            sr.close();
        }
    }
}
