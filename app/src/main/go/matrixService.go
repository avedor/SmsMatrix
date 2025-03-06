package smsmatrix

import (
	"fmt"
	"strconv"
)

type MatrixService struct {
	mx          *Matrix
	botUsername string
	botPassword string
	username    string
	device      string
	hsUrl       string
	syncDelay   string
	syncTimeout string
	mms         *MMSMonitor
	mChannelId  string
	TAG         string
}

func NewMatrixService() *MatrixService {
	return &MatrixService{
		mChannelId: "",
		TAG:        "MatrixService",
	}
}

func (s *MatrixService) StartService(botUsername, botPassword, username, device, hsUrl, syncDelay, syncTimeout string) {
	s.botUsername = botUsername
	s.botPassword = botPassword
	s.username = username
	s.device = device
	s.hsUrl = hsUrl
	s.syncDelay = syncDelay
	s.syncTimeout = syncTimeout

	// Convert to integers
	delayInt, _ := strconv.Atoi(s.syncDelay)
	timeoutInt, _ := strconv.Atoi(s.syncTimeout)

	if s.mx == nil && s.botUsername != "" && s.botPassword != "" && s.username != "" && s.device != "" && s.hsUrl != "" {
		s.mx = NewMatrix(delayInt, timeoutInt)
		s.mx.DeviceName = s.device
		s.mx.BotUsername = s.botUsername
		s.mx.BotHSUrl = s.hsUrl
		s.mx.RealUserId = s.username

		fmt.Println("Service started with HS URL:", s.hsUrl)
	} else if s.mx == nil {
		fmt.Println("Missing Information")
		return
	}

	// Handle incoming SMS/MMS (this is where you'd receive Android intents)
	phone, msgType, body := GetIncomingSmsDetails() // Mocked function
	if phone != "" {
		if msgType == "text" || msgType == "notice" {
			s.mx.SendMessage(phone, body, msgType)
		} else if msgType == "image" || msgType == "video" {
			fileData, fileName, contentType := GetIncomingFileDetails() // Mocked function
			s.mx.SendFile(phone, fileData, msgType, fileName, contentType)
		}
	}

	// Start MMS Monitoring
	if s.mms == nil {
		s.mms = NewMMSMonitor()
		s.mms.StartMMSMonitoring()
	}
}

func (s *MatrixService) StopService() {
	if s.mx != nil {
		s.mx.Destroy()
		s.mx = nil
	}
	if s.mms != nil {
		s.mms.StopMMSMonitoring()
		s.mms = nil
	}
}
