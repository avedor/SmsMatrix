package smsmatrix

import "log"

type MMSMonitor struct {
	MatrixService   interface{} // mainActivity
	contentResolver interface{} // ContentResolver
	mainContext     interface{} // Context
	mmshandler      interface{} // Handler
	mmsObserver     interface{} // ContentObserver
	monitorStatus   bool
	mmsCount        int
	TAG             string
}

func NewMMSMonitor() *MMSMonitor {
	return &MMSMonitor{
		monitorStatus: false,
		mmsCount:      0,
		TAG:           "MMSMonitor",
	}
}

func (m *MMSMonitor) StartMMSMonitoring() {
	log.Println("Starting MMS Monitoring")
}

func (m *MMSMonitor) StopMMSMonitoring() {
	if m.monitorStatus {
		log.Printf("%s", m.TAG)
	}
}
