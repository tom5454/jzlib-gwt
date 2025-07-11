package java.util.zip;

import com.jcraft.jzlib.ZStream;

@SuppressWarnings("deprecation")
public class ZStreamRef {
	private ZStream zs;

	public ZStreamRef(ZStream init) {
		this.zs = init;
	}

	public void clear() {
		zs = null;
	}

	public ZStream address() {
		return zs;
	}
}
