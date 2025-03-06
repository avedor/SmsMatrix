package smsmatrix

import "log"

// Define Go struct to represent Matrix
type Matrix struct {
	SyncDelay   int
	SyncTimeout int
	HSConfig    interface{} // Replace with actual type if necessary
	Context     interface{} // Replace with actual type if necessary
	Session     interface{} // Replace with actual type if necessary
	Transaction int
	Tag         string
	NotSendMsgs []string    // Use a simple list for example
	DH          interface{} // Replace with actual type if necessary
	EvLis       interface{} // Replace with actual type if necessary
	Store       interface{} // Replace with actual type if necessary
	DeviceName  string
	BotUsername string
	BotHSUrl    string
	RealUserId  string
}

// Constructor function to initialize the struct
func NewMatrix(syncDelay, syncTimeout int) *Matrix {
	return &Matrix{
		SyncDelay:   syncDelay,
		SyncTimeout: syncTimeout,
		Tag:         "Matrix",
	}
}

func (m *Matrix) SendMessage() {}

func (m *Matrix) SendFile(phone string, fileData, msgType, fileName, contentType) {}

func (m *Matrix) Destroy() {
	log.Println("Destroying Matrix")
	// session.stopEventStream()
	// dh.removeListener(evLis)
	// store.close()
}

// SetSyncDelay function that operates on Matrix struct
func (m *Matrix) SetSyncDelay(syncDelay int) {
	m.SyncDelay = syncDelay
}

// SetSyncTimeout function that operates on Matrix struct
func (m *Matrix) SetSyncTimeout(syncTimeout int) {
	m.SyncTimeout = syncTimeout
}

// GetSyncDelay function that returns the sync delay
func (m *Matrix) GetSyncDelay() int {
	return m.SyncDelay
}

// AddMessage adds a message to the NotSendMsgs list
func (m *Matrix) AddMessage(msg string) {
	m.NotSendMsgs = append(m.NotSendMsgs, msg)
}

// GetMessages returns the list of unsent messages
func (m *Matrix) GetMessages() []string {
	return m.NotSendMsgs
}

// ----------------------------
// Function to create a new Matrix instance
func CreateMatrix(syncDelay, syncTimeout int) *Matrix {
	return NewMatrix(syncDelay, syncTimeout)
}

// Function to set the sync delay
func SetMatrixSyncDelay(matrix *Matrix, syncDelay int) {
	matrix.SetSyncDelay(syncDelay)
}

// Function to set the sync timeout
func SetMatrixSyncTimeout(matrix *Matrix, syncTimeout int) {
	matrix.SetSyncTimeout(syncTimeout)
}

// Function to get the sync delay
func GetMatrixSyncDelay(matrix *Matrix) int {
	return matrix.GetSyncDelay()
}

// Function to add a message to the unsent message list
func AddMessageToMatrix(matrix *Matrix, msg string) {
	matrix.AddMessage(msg)
}

// Function to retrieve all unsent messages
func GetUnsentMessages(matrix *Matrix) []string {
	return matrix.GetMessages()
}
