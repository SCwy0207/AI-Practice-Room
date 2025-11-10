package com.victor.ai_practice_room;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.*;

@SpringBootTest
class AiPracticeRoomApplicationTests {

    @Test
    void contextLoads() {
        try(FileInputStream fileInputStream = new FileInputStream("D:\\Alex\\Desktop\\Image_20251107164024_87_82.png");
            FileOutputStream fileOutputStream = new FileOutputStream("D:\\Alex\\Desktop\\白厄.png");
        ){
            int len;
            byte[] buffer = new byte[20];
            while((len=fileInputStream.read(buffer))!=-1){
                fileOutputStream.write(buffer,0,len);
            }
        }catch (IOException e){
            System.out.println(e.getMessage());
        }
    }
    //使用字符流传照片的错误用法
    @Test
    public void test(){
        try(
                FileReader fileReader = new FileReader("D:\\Alex\\Desktop\\Image_20251107164024_87_82.png");
                FileWriter fileWriter = new FileWriter("D:\\Alex\\Desktop\\白厄.png");
                ){
            int len;
            while((len=fileReader.read())!=-1){
                fileWriter.write(len);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
