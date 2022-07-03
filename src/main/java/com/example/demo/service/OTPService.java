package com.example.demo.service;

import java.io.UnsupportedEncodingException;
import java.util.Random;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;

@Service
public class OTPService {

	@Autowired
	JavaMailSender mailSender;
	
	@Autowired
	private UserRepository userRepository;


	public void generateOneTimePassword(User user) throws UnsupportedEncodingException, MessagingException {
		int otp = new Random().nextInt(1000,2000);
		user.setOneTimePassword(otp);
		// user.setOtpRequestedTime(new Date());
		userRepository.save(user);
		sendOTPEmail(user, otp);
	}

	public void sendOTPEmail(User user, int OTP) throws UnsupportedEncodingException, MessagingException {
		MimeMessage message = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message);

		helper.setFrom("parmardhaval2000@gmail.com", " Support");
		helper.setTo(user.getEmail());

		String subject = "Here's your One Time Password (OTP) - Expire in 5 minutes!";

		String content = "<p>Hello </p>" + "<p>For security reason, you're required to use the following "
				+ "One Time Password to login:</p>" + "<p><b>" + OTP + "</b></p>" + "<br>"
				+ "<p>Note: this OTP is set to expire in 5 minutes.</p>";

		helper.setSubject(subject);

		helper.setText(content, true);

		mailSender.send(message);
	}

	public void clearOTP(User user) {
		user.setOneTimePassword(0);
		// customer.setOtpRequestedTime(null);
		userRepository.save(user);
	}

}
