@GrabResolver(name='nr', root='https://oss.sonatype.org/service/local/repositories/releases/content/')
@Grab(group='com.neuronrobotics', module='SimplePacketComsJava', version='0.12.0')

import com.neuronrobotics.sdk.addons.kinematics.AbstractLink
import com.neuronrobotics.sdk.addons.kinematics.AbstractRotoryLink
import com.neuronrobotics.sdk.addons.kinematics.INewLinkProvider
import com.neuronrobotics.sdk.addons.kinematics.LinkConfiguration
import com.neuronrobotics.sdk.addons.kinematics.LinkFactory
import com.neuronrobotics.sdk.addons.kinematics.MobileBase
import com.neuronrobotics.sdk.addons.kinematics.imu.IMUUpdate
import com.neuronrobotics.sdk.common.BowlerAbstractDevice
import com.neuronrobotics.sdk.common.DeviceManager
import com.neuronrobotics.sdk.common.IDeviceAddedListener
import com.neuronrobotics.sdk.common.NonBowlerDevice
import edu.wpi.SimplePacketComs.phy.UDPSimplePacketComs;
import edu.wpi.SimplePacketComs.*;

public class SimpleServoUDPServo extends UDPSimplePacketComs {
	private PacketType servos = new edu.wpi.SimplePacketComs.BytePacketType(1962, 64);
	private final byte[] data = new byte[16];
	public SimpleServoUDPServo(def address) {
		super(address);
		servos.waitToSendMode();
		addPollingPacket(servos);
		addEvent(1962, {
			writeBytes(1962, data);
		});
	}
	public byte[] getDataUp() {
		//return servos.getUpstream();
		return data;
	}
	public byte[] getData() {
		return data;
	}
}

public class SimpleServoUDPImu extends UDPSimplePacketComs {
	private PacketType imuData = new edu.wpi.SimplePacketComs.FloatPacketType(1804, 64);
	private final double[] status = new double[12];
	
	public SimpleServoUDPImu(def address) {
		super(address);
		addPollingPacket(imuData);
		addEvent(1804, {
			readFloats(1804,status);
		});
	}
	public double[] getImuData() {
		return status;
	}
}
public class VirtualDeviceSimple extends AbstractSimpleComsDevice{
	public  int read(byte[] message, int howLongToWaitBeforeTimeout) {
	}

	public  int write(byte[] message, int length, int howLongToWaitBeforeTimeout){
	}

	public  boolean disconnectDeviceImp(){
	}

	public  boolean connectDeviceImp(){
		return false;
	}
}

public class SimpleServoUDPServoVirt extends VirtualDeviceSimple {
	private PacketType servos = new edu.wpi.SimplePacketComs.BytePacketType(1962, 64);
	private final byte[] data = new byte[16];
	public SimpleServoUDPServoVirt() {
		servos.waitToSendMode();
		addPollingPacket(servos);
		addEvent(1962, {
			writeBytes(1962, data);
		});
	}
	public byte[] getDataUp() {
		//return servos.getUpstream();
		return data;
	}
	public byte[] getData() {
		return data;
	}
}

public class SimpleServoUDPImuVirt extends VirtualDeviceSimple {
	private PacketType imuData = new edu.wpi.SimplePacketComs.FloatPacketType(1804, 64);
	private final double[] status = new double[12];
	
	public SimpleServoUDPImuVirt() {
		addPollingPacket(imuData);
		addEvent(1804, {
			readFloats(1804,status);
		});
	}
	public double[] getImuData() {
		return status;
	}
}
public class HIDSimpleComsDevice extends NonBowlerDevice{
	AbstractSimpleComsDevice simple;
	AbstractSimpleComsDevice simpleServo;
	public HIDSimpleComsDevice(def simp, def servo){
		simple = simp
		simpleServo=servo
		setScriptingName("hidbowler")
	}
	@Override
	public  void disconnectDeviceImp(){
		simple.disconnect()
		simpleServo.disconnect()
		println "HID device Termination signel shutdown"
	}
	
	@Override
	public  boolean connectDeviceImp(){
		simple.connect()
		simpleServo.connect()
	}
	void setValue(int i,int position){
		if(position<0){
			println "Link commanded to invalid position! "+position
			position=0;	
		}	
		if(position>180){
			println "Link commanded to invalid position! "+position
			position=180;	
		}		
		simpleServo.getData()[i]=(byte)position;
		simpleServo.servos.pollingMode();
	}
	int getValue(int i){
		if(simpleServo.getDataUp()[i]>=0)
			return simpleServo.getDataUp()[i]
		return ((int)simpleServo.getDataUp()[i])+256
	}
	public float[] getImuData() {
		return simple.getImuData();
	}
	@Override
	public  ArrayList<String>  getNamespacesImp(){
		// no namespaces on dummy
		return [];
	}
	
	
}
public class HIDRotoryLink extends AbstractRotoryLink{
	HIDSimpleComsDevice device;
	int index =0;
	int lastPushedVal = 0;
	private static final Integer command =1962
	/**
	 * Instantiates a new HID rotory link.
	 *
	 * @param c the c
	 * @param conf the conf
	 */
	public HIDRotoryLink(HIDSimpleComsDevice c,LinkConfiguration conf,String devName) {
		super(conf);
		index = conf.getHardwareIndex()
		device=c
		if(device ==null)
			throw new RuntimeException("Device can not be null")
		conf.setDeviceScriptingName(devName)
		c.simpleServo.addEvent(command,{
			int val= getCurrentPosition();
			if(lastPushedVal!=val){
				//println "Fire Link Listner "+index+" value "+getCurrentPosition()
				try{
				fireLinkListener(val);
				}catch(Exception e){}
				lastPushedVal=val
			}else{
				//println index+" value same "+getCurrentPosition()
			}
			
		})
		
	}

	/* (non-Javadoc)
	 * @see com.neuronrobotics.sdk.addons.kinematics.AbstractLink#cacheTargetValueDevice()
	 */
	@Override
	public void cacheTargetValueDevice() {
		device.setValue(index,(int)getTargetValue())
	}

	/* (non-Javadoc)
	 * @see com.neuronrobotics.sdk.addons.kinematics.AbstractLink#flush(double)
	 */
	@Override
	public void flushDevice(double time) {
		// auto flushing
	}
	
	/* (non-Javadoc)
	 * @see com.neuronrobotics.sdk.addons.kinematics.AbstractLink#flushAll(double)
	 */
	@Override
	public void flushAllDevice(double time) {
		// auto flushing
	}

	/* (non-Javadoc)
	 * @see com.neuronrobotics.sdk.addons.kinematics.AbstractLink#getCurrentPosition()
	 */
	@Override
	public double getCurrentPosition() {
		return (double)device.getValue(index);
	}

}
class MyDeviceListener implements IDeviceAddedListener{
	String searchName
	public MyDeviceListener(String searchName){
		this.searchName=searchName
	}
	public void onNewDeviceAdded(BowlerAbstractDevice bad) {
		// wait for the mobile base to be added to the device manager then add the event listenr for the IMU
		if(MobileBase.class.isInstance(bad)) {
			println "Adding the IMU callback from the device manager listener"
			HIDSimpleComsDevice d = DeviceManager.getSpecificDevice( searchName)
			MobileBase m =(MobileBase)bad;
			d.simple.addEvent(1804, {
					double[] imuDataValues = d.simple.getImuData()
					m.getImu()
					.setHardwareState(
							new IMUUpdate(
								-imuDataValues[9],	-imuDataValues[11],	-imuDataValues[10],
							   imuDataValues[3],//Double rotxAcceleration,
							   imuDataValues[4],//Double rotyAcceleration,
							   imuDataValues[5],//Double rotzAcceleration
					   ))
					
					
			   });
		   
			DeviceManager.removeDeviceAddedListener(this);
		}
	}

	public void onDeviceRemoved(BowlerAbstractDevice bad) {
		// TODO Auto-generated method stub
		
	}
}
INewLinkProvider provider= new INewLinkProvider() {
	public AbstractLink generate(LinkConfiguration conf) {
		String searchName = conf.getDeviceScriptingName();
		def dev = DeviceManager.getSpecificDevice( searchName,{
			//If the device does not exist, prompt for the connection
			def simp = null;
			def srv = null
			HashSet<InetAddress> addresses = UDPSimplePacketComs.getAllAddresses(searchName);
			println addresses
			if (addresses.size() >= 1){
				println "Servo Servers at "+addresses
				simp = new SimpleServoUDPImu(addresses.toArray()[0])
				simp.setReadTimeout(20);
				srv = new SimpleServoUDPServo(addresses.toArray()[0])
				srv.setReadTimeout(20);
				
			}else {
				simp = new SimpleServoUDPImuVirt()
				simp.setReadTimeout(20);
				srv = new SimpleServoUDPServoVirt()
				srv.setReadTimeout(20);
			}
			HIDSimpleComsDevice d = new HIDSimpleComsDevice(simp,srv)
			d.connect(); // Connect to it.
			if(simp.isVirtual()){
				println "\n\n\nDevice is in virtual mode!\n\n\n"
			}
			println "Connecting new device: "+searchName
			DeviceManager.addDeviceAddedListener(new MyDeviceListener(searchName))
			return d
		})
		
		return new HIDRotoryLink(dev,conf,searchName);
	}
	
}

if(args==null)
	args=["hidfast"]
LinkFactory.addLinkProvider(args[0], provider)

return provider












