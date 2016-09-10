from org.csstudio.display.builder.runtime.script import PVUtil, ScriptUtil

secs = PVUtil.getDouble(pvs[0])
max_time = PVUtil.getDouble(pvs[1])
sound = PVUtil.getInt(pvs[2]) > 0

def startSound():
    global audio
    if sound  and (not 'audio' in globals()  or  audio is None):
        print("Play Sound")
        audio = ScriptUtil.playAudio(widget, "timer.wav")
    
def stopSound():
    global audio
    if sound and 'audio' in globals()  and  audio is not None:
        print("End Sound")
        audio.cancel(True)
        audio = None
 
fraction = secs/max_time
widget.setPropertyValue("total_angle", 360.0*fraction)

if fraction < 0.25:
    color = [ 255, 0, 0 ]
    startSound()
elif fraction < 0.4:
    color = [ 255, 255, 0 ]
    startSound()
else:
    color = [ 0, 255, 0 ]
    stopSound()
widget.setPropertyValue("background_color", color)

