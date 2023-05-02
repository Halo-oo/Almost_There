package com.almostThere.domain.meeting.controller;

import com.almostThere.domain.meeting.dto.ReceiptResponseDto;
import com.almostThere.domain.meeting.dto.create.CalculateDetailRequestDto;
import com.almostThere.domain.meeting.service.CalculateDetailService;
import com.almostThere.global.response.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

@RestController
@CrossOrigin
@RequiredArgsConstructor
@RequestMapping("/meeting-calculate")
public class MeetingCalculateApiController {

    private final CalculateDetailService calculateDetailService;

    /**
     * dyeon7310
     * @param receipt
     * @return 영수증 이미지 파일을 읽어 상호명과 총금액을 구한다.
     * CLOVA OCR Document API 이용
     * 허용 파일 형식: JPG, JPEG, PNG
     */
    @PostMapping("/receipt")
    public BaseResponse getReceiptInfo(@RequestParam MultipartFile receipt) {
        String apiURL = "https://v2crp4slfh.apigw.ntruss.com/custom/v1/22190/b0d9c109697c19ef5d90ba10100ec6bccc7087843fee903fd0b37c5a76729753/document/receipt";
        String secretKey = "VlN2d2VQQktFRWdsdnRrVFh3dk1xdGtGekdNem9PYnI=";
        String contentType = receipt.getContentType();

        if (receipt==null || !(contentType.equals("image/jpg") || contentType.equals("image/jpeg") || contentType.equals("image/png"))){
            return BaseResponse.invalidFile();
        }

        try {
            URL url = new URL(apiURL);
            HttpURLConnection con = (HttpURLConnection)url.openConnection();
            con.setUseCaches(false);
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            con.setRequestProperty("X-OCR-SECRET", secretKey);

            System.out.println("파일 형식: "+contentType.substring(6));
            JSONObject json = new JSONObject();
            json.put("version", "V2");
            json.put("requestId", UUID.randomUUID().toString());
            json.put("timestamp", System.currentTimeMillis());
            JSONObject image = new JSONObject();
            image.put("format", "jpg");
            //image should be public, otherwise, should use data
            FileInputStream inputStream = (FileInputStream) receipt.getInputStream();
            byte[] buffer = new byte[inputStream.available()];
            inputStream.read(buffer);
            inputStream.close();
            image.put("data", buffer);
            image.put("name", "demo");
            JSONArray images = new JSONArray();
            images.put(image);
            json.put("images", images);
            String postParams = json.toString();

            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(postParams);
            wr.flush();
            wr.close();

            int responseCode = con.getResponseCode();
            BufferedReader br;
            if (responseCode == 200) {
                br = new BufferedReader(new InputStreamReader(con.getInputStream()));
            } else {
                br = new BufferedReader(new InputStreamReader(con.getErrorStream()));
            }
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = br.readLine()) != null) {
                response.append(inputLine);
            }
            br.close();

            ReceiptResponseDto result = calculateDetailService.parseData(response);
            return BaseResponse.success(result);

        } catch (Exception e) {
            return BaseResponse.fail();
        }
    }

    @PostMapping("/detail")
    public BaseResponse addCalculateDetail(@RequestBody CalculateDetailRequestDto detailDto){
        calculateDetailService.saveCalculateDetail(detailDto);  //정산 내역 저장
        return BaseResponse.success(null);
    }
}

