import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ImageOverlayProgram {

    public static void main(String[] args) {
        //args = new String[] {"sv", "\"C:\\Users\\Paulf\\Downloads\\Flag", "of","the","United","States.png\""};
        // --- Default Input Values (replace with actual inputs or command line arguments) ---
        String imagePath1 = "en"; // Path to the first PNG
        int i = 0;
        if(args.length > i){
            imagePath1 = args[i];
            i++;
            if(imagePath1.startsWith("\"")){
                do{
                    imagePath1 += " " + args[i];
                    i++;
                }while(!imagePath1.endsWith("\"") && args.length != i);
                if(!imagePath1.endsWith("\"")){
                    System.err.println("Incorrect image path");
                    return;
                }else{
                    imagePath1 = imagePath1.substring(1, imagePath1.length()-1);
                }
            }
        }
        String imagePath2 = "de"; // Path to the second PNG
        if(args.length > i){
            imagePath2 = args[i];
            i++;
            if(imagePath2.startsWith("\"")){
                do{
                    imagePath2 += " " + args[i];
                    i++;
                }while(!imagePath2.endsWith("\"") && args.length != i);
                if(!imagePath2.endsWith("\"")){
                    System.err.println("Incorrect image path");
                    return;
                }else{
                    imagePath2 = imagePath2.substring(1, imagePath2.length()-1);
                }
            }
        }
        float height2 = 0.8f;
        double scaleFactor1 = 1;  // Scaling factor for the first image
        double scaleFactor2 = 0.2;  // Scaling factor for the second image
        int outlineWidth = 25;       // Width of the white outline around the second image
        if(args.length > i){
            outlineWidth = Integer.parseInt(args[i]);
            i++;
        }

        if(i != args.length){
            System.err.println("Incorrect number of arguments");
            return;
        }

        // --- Output dimensions ---
        int outputSize = 1024;

        try {
            if(!((new File(imagePath1 + ".png").exists() || new File(imagePath1).exists()) && (new File(imagePath2 + ".png").exists() || new File(imagePath2).exists()))){
                System.err.println("Error: Could not load one or both images. Please check file paths or language chars.");
                return;
            }

            BufferedImage image1;
            BufferedImage image2;
            try {
                // 1. Load the two PNG images
                if(new File(imagePath1 + ".png").exists()) image1 = ImageIO.read(new File(imagePath1 + ".png"));
                else image1 = ImageIO.read(new File(imagePath1));
                if(new File(imagePath2 + ".png").exists()) image2 = ImageIO.read(new File(imagePath2 + ".png"));
                else image2 = ImageIO.read(new File(imagePath2));
            }catch (IOException e){
                System.err.println("Error: Could not load one or both images. Please check file paths or language chars.");
                return;
            }

            if (image1 == null || image2 == null) {
                System.err.println("Error: Could not load one or both images. Please check file paths or language chars.");
                return;
            }

            // 2. Create the output image with a transparent background
            BufferedImage outputImage = new BufferedImage(outputSize, outputSize, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = outputImage.createGraphics();

            // Enable anti-aliasing for smoother graphics
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            // Fill with transparency (not strictly necessary as TYPE_INT_ARGB defaults to transparent)
            // g2d.setBackground(new Color(0, 0, 0, 0));
            // g2d.clearRect(0, 0, outputSize, outputSize);

            // 3. Scale the first image
            int scaledWidth1 = (int) (image1.getWidth() * scaleFactor1 / ((double) image1.getWidth() / outputSize));
            int scaledHeight1 = (int) (image1.getHeight() * scaleFactor1 / ((double) image1.getWidth() / outputSize));
            BufferedImage scaledImage1 = new BufferedImage(scaledWidth1, scaledHeight1, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g1 = scaledImage1.createGraphics();
            g1.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g1.drawImage(image1, 0, 0, scaledWidth1, scaledHeight1, null);
            g1.dispose();

            // 4. Calculate position to center the first image on the output canvas
            int x1 = (outputSize - scaledWidth1) / 2;
            int y1 = (outputSize - scaledHeight1) / 2;

            // 5. Draw the scaled first image onto the output image
            g2d.drawImage(scaledImage1, x1, y1, null);

            // 6. Scale the second image
            int scaledWidth2 = (int) (image2.getWidth() * scaleFactor2 / ((double) image2.getHeight() / outputSize));
            int scaledHeight2 = (int) (image2.getHeight() * scaleFactor2 / ((double) image2.getHeight() / outputSize));
            BufferedImage scaledImage2 = new BufferedImage(scaledWidth2, scaledHeight2, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = scaledImage2.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(image2, 0, 0, scaledWidth2, scaledHeight2, null);
            g2.dispose();

            // 7. Calculate the top-left position for the second image so its center is at (centerX, centerY)
            //    relative to the top-left of where the *original unscaled* image1 would be if it were
            //    drawn at (x1,y1) and then adjusted for its scaling.
            //    More simply, if the input (centerX, centerY) is meant to be relative to the *output canvas*:
            int x2 = (int) (outputSize - scaledWidth2 - outlineWidth);
            int y2 = (int) (height2 * outputSize - scaledImage2.getHeight()/2f);


            // 8. Draw the white outline for the second image
            if (outlineWidth > 0) {
                g2d.setColor(Color.WHITE);
                // Create a slightly larger shape for the outline
                // This can be done by drawing the image multiple times slightly offset,
                // or by creating a mask and dilating it, or by drawing a thicker shape.
                // A simpler approach for a rectangular image is to draw thicker lines or a larger rect.

                // For non-rectangular transparent PNGs, a common technique is to create a
                // monochrome version of the image, make it white, and draw it offset in several directions.
                // Or, draw the image multiple times, slightly offset.

                // More robust outline for transparent images:
                BufferedImage outlineLayer = new BufferedImage(outputSize, outputSize, BufferedImage.TYPE_INT_ARGB);
                Graphics2D olg = outlineLayer.createGraphics();
                olg.setComposite(AlphaComposite.SrcOver); // Ensure proper blending

                // Draw the scaled image multiple times, offset by the outline width
                for (int oy = -outlineWidth; oy <= outlineWidth; oy++) {
                    for (int ox = -outlineWidth; ox <= outlineWidth; ox++) {
                        // Only draw if it's part of the "outer ring" for a more rounded effect,
                        // or simply draw all for a blockier outline. For simplicity, draw all.
                        // if (Math.abs(ox) == outlineWidth || Math.abs(oy) == outlineWidth) {
                        olg.drawImage(scaledImage2, x2 + ox, y2 + oy, null);
                        // }
                    }
                }
                olg.dispose();

                // Create a version of the outlineLayer that is solid white where there are pixels
                BufferedImage whiteOutline = new BufferedImage(outputSize, outputSize, BufferedImage.TYPE_INT_ARGB);
                Graphics2D wog = whiteOutline.createGraphics();
                g2d.setColor(new Color(0, 255, 255));
                g2d.setComposite(AlphaComposite.Clear);
                for (int r = 0; r < outputSize; r++) {
                    for (int c = 0; c < outputSize; c++) {
                        if (outlineLayer.getRGB(c, r) != 0) { // if pixel is not transparent
                            g2d.fillRect(c, r, 1, 1);
                        }
                    }
                }
                wog.dispose();
//                g2d.setComposite(AlphaComposite.SrcOver);
//                g2d.drawImage(whiteOutline, 0, 0, null);
                g2d.setComposite(AlphaComposite.SrcOver);
            }


            // 9. Draw the scaled second image on top
            g2d.drawImage(scaledImage2, x2, y2, null);

            // Dispose of the graphics context
            g2d.dispose();

            // 10. Save the result as test.png
            File outputFile = new File("test.png");
            ImageIO.write(outputImage, "png", outputFile);

            System.out.println("Image processing complete. Output saved to " + outputFile.getAbsolutePath());

        } catch (IOException e) {
            System.err.println("An error occurred during image processing: " + e.getMessage());
            e.printStackTrace();
        }
    }
}