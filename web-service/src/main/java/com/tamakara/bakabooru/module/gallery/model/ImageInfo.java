package com.tamakara.bakabooru.module.gallery.model;

import lombok.Getter;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * 閸ュ墽澧栨穱鈩冧紖鐏忎浇顥婄猾?
 * <p>
 * 閺嬪嫰鈧姵妞傞懛顏勫З鐠囪褰囬弬鍥︽婢舵潙鍨庨弸鎰啍妤傛ǜ鈧焦鐗稿蹇ョ礉楠炴儼顓哥粻?SHA256 閸濆牆绗囬妴?
 * 婵″倹鐏夐弬鍥︽娑撳秵妲搁崶鍓у閿涘本鐎柅鐘插毐閺侀绱伴幎娑樺毉瀵倸鐖堕妴?
 */
@Getter
public class ImageInfo {

    private final int width;
    private final int height;
    private final long size;
    private final String format;      // 閻喎鐤勯惃鍕禈閻楀洦鐗稿?(jpeg, png, gif)
    private final String extension;   // 瀵ら缚顔呴惃鍕瀮娴犺泛鎮楃紓鈧?(jpg, png)
    private final boolean isAnimated; // 閺勵垰鎯佹稉鍝勫З閸?(GIF/WebP)

    /**
     * 閺嬪嫰鈧姴鍤遍弫甯窗娴肩姴鍙嗛弬鍥︽閿涘矁鍤滈崝銊ュ瀻閺?
     * @param file 閺堫剙婀撮弬鍥︽鐎电钖?
     * @throws IllegalArgumentException 婵″倹鐏夐弬鍥︽娑撳秴鐡ㄩ崷銊﹀灗娑撳秵妲搁崶鍓у
     * @throws RuntimeException 婵″倹鐏夌拠璇插絿鏉╁洨鈻兼稉顓炲絺閻?IO 闁挎瑨顕?
     */
    public ImageInfo(File file) {
        if (file == null || !file.exists() || file.isDirectory()) {
            throw new IllegalArgumentException("閺傚洣娆㈡稉宥呯摠閸︺劍鍨ㄧ捄顖氱窞閺冪姵鏅?");
        }

        this.size = file.length();

        // 2. 鐟欙絾鐎介崶鍓у閸忓啯鏆熼幑?
        try (ImageInputStream in = ImageIO.createImageInputStream(file)) {
            if (in == null) {
                throw new RuntimeException("閺冪姵纭剁拠璇插絿閸ュ墽澧栧ù?");
            }

            // 閼奉亜濮╃€电粯澹樼憴锝囩垳閸?
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) {
                throw new IllegalArgumentException("閺冪姵纭剁拠鍡楀焼閻ㄥ嫭鏋冩禒鑸电壐瀵骏绱濋棃鐐寸垼閸戝棗娴橀悧?");
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(in);

                // 閼惧嘲褰囬惇鐔风杽閺嶇厧绱?
                this.format = reader.getFormatName().toLowerCase();
                // 缁犫偓閸楁洜娈戦崥搴ｇ磻閺勭姴鐨?
                this.extension = mapFormatToExtension(this.format);

                // 閼惧嘲褰囬悧鈺冩倞鐏忓搫顕?(娑撳秷袙閻礁鍎氱槐?
                this.width = reader.getWidth(0);
                this.height = reader.getHeight(0);

                // 缁犫偓閸楁洜娈戦崝銊ユ禈濡偓濞?(婵″倹鐏夐張澶庣Т鏉?鐢嶇礉闁艾鐖堕弰顖氬З閸?
                int frameCount = 1;
                try {
                    // true 閸忎浇顔忛幍顐ｅ伎閺傚洣娆㈠ù浣规降绾喖鍨忕拋锛勭暬鐢勬殶閿涘牆顕?GIF 缁嬪秵鍙冩担鍡楀櫙绾噯绱?
                    // 婵″倹鐏夋潻鑺ョ湴閺嬩浇鍤ч柅鐔峰閿涘苯褰叉禒銉啎娑?false閿涘本鍨ㄩ懓鍛涧鐎?gif/webp 閺嶇厧绱￠幍褑顢戝銈嗩梾閺?
                    if ("gif".equals(this.format) || "webp".equals(this.format)) {
                        frameCount = reader.getNumImages(true);
                    }
                } catch (Exception ignored) {
                    // 閺屾劒绨洪弽鐓庣础娑撳秵鏁幐浣筋吀缁犳鎶氶弫甯礉韫囩晫鏆?
                }
                this.isAnimated = frameCount > 1;

            } finally {
                reader.dispose(); // 闁插﹥鏂?reader
            }
        } catch (IOException e) {
            throw new RuntimeException("閸ュ墽澧栫憴锝嗙€芥径杈Е: 閺傚洣娆㈤崣顖濆厴瀹稿弶宕崸?", e);
        }

        // 3. 閺堚偓缂佸牊鐗庢?
        if (this.width <= 0 || this.height <= 0) {
            throw new IllegalArgumentException("閺冪姵鏅ラ惃鍕禈閻楀洤鏄傜€? " + width + "x" + height);
        }
    }

    private String mapFormatToExtension(String formatName) {
        return switch (formatName) {
            case "jpeg" -> "jpg";
            case "wbmp" -> "bmp";
            default -> formatName;
        };
    }
}
