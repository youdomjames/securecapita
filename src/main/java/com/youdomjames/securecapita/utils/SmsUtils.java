package com.youdomjames.securecapita.utils;

import com.vonage.client.VonageClient;
import com.vonage.client.sms.MessageStatus;
import com.vonage.client.sms.SmsSubmissionResponse;
import com.vonage.client.sms.messages.TextMessage;
import com.youdomjames.securecapita.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SmsUtils {
    private  final VonageClient vonageClient;
    public static final String BRAND_NAME = "17787802740";

    public void sendSMS(String receiverFirstName, String receiverLastName, String receiverPhoneNumber, String messageBody){
        TextMessage textMessage = new TextMessage(BRAND_NAME, receiverPhoneNumber, messageBody);

        SmsSubmissionResponse response = vonageClient.getSmsClient().submitMessage(textMessage);
        if (response.getMessages().get(0).getStatus() == MessageStatus.OK) {
            log.info("Message sent successfully to: {} {}", receiverFirstName, receiverLastName);
        } else {
            log.error("Message failed with error: " + response.getMessages().get(0).getErrorText());
            throw new ApiException("Something went wrong while sending message. Please try again later");
        }
    }
}
