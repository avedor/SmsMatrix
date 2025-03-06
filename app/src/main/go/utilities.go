package smsmatrix

import (
	"log"
	"os/exec"
)

// SendMatrix sends an SMS event to MatrixService.
// This must be an exported function (capitalized) to be accessible in Java/Kotlin.
func SendMatrix(phone, body, msgType, fileName, contentType string) {
	log.Println("Arrived in SendMatrix")

	cmd := exec.Command("/system/bin/sh", "-c",
		`am start-foreground-service -a com.averydorgan.smsmatrix.SEND_MATRIX -n com.averydorgan.smsmatrix/.MatrixService --es SendSms_phone "`+phone+`" --es SendSms_body "`+body+`" --es SendSms_type "`+msgType+`" --es SendSms_fileName "`+fileName+`" --es SendSms_contentType "`+contentType+`"`,
	)

	log.Println("Running SendMatrix Command")
	if err := cmd.Run(); err != nil {
		log.Println("Failed to start MatrixService:", err)
	}
}
