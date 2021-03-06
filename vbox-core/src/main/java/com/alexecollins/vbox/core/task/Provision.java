package com.alexecollins.vbox.core.task;

import au.com.bytecode.opencsv.CSVReader;
import com.alexecollins.util.DurationUtils;
import com.alexecollins.util.ExecUtils;
import com.alexecollins.vbox.core.ScanCodes;
import com.alexecollins.vbox.core.Snapshot;
import com.alexecollins.vbox.core.VBox;
import com.alexecollins.vbox.core.Work;
import com.alexecollins.vbox.provisioning.Provisioning;
import org.apache.commons.lang.ArrayUtils;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ResourceHandler;
import org.mortbay.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class Provision extends AbstractTask {
	private static final Logger LOGGER = LoggerFactory.getLogger(Provision.class);
	private final Server server;
	private final Set<String> targets;

	public Provision(Work work, VBox box, Set<String> targets) throws IOException {
		super(work, box);
		this.targets = targets;

		server = new Server(Provision.findFreePort());
	}

	public Void call() throws Exception {

		if (box.exists()) {
			new Stop(box).call();
		}

		verifySignature();

		final Snapshot snapshot = Snapshot.POST_PROVISIONING;
		if (box.exists()) {
			box.powerOff();
			if (box.getSnapshots().contains(snapshot)) {
				LOGGER.info("restoring '" + box.getName() + "' from snapshot " + snapshot);
				box.restoreSnapshot(Snapshot.POST_PROVISIONING);
				return null;
			}
		}

		// if the box doesn't exist, create it
		if (!box.exists()) {
			new Create(work, box).call();
		}

		LOGGER.info("provisioning '" + box.getName() + "'");

		// TODO startServer(box);
		try {
			final List<Provisioning.Target> targets = box.getProvisioning().getTarget();
			for (Provisioning.Target target : targets) {
				if (this.targets.contains(target.getName()) || this.targets.equals(Collections.<String>singleton("*"))) {
					if (target.equals(targets.get(0)) && !box.getProperties().getProperty("VMState").equals("running")) {
						LOGGER.info("starting box");
						box.start();
					}
					executeTarget(box, target);
					if (target.equals(targets.get(targets.size() - 1))) {
						if (box.getProperties().getProperty("VMState").equals("running")) {
							LOGGER.info("stopping box");
							box.pressPowerButton();
							box.awaitState((long) 10000, "poweroff");
						}
						box.takeSnapshot(snapshot);
					}
				} else {
					LOGGER.info("skipping target " + target.getName());
				}
			}
		} finally {
			// TODO uncomment - stopServer();
		}
		return null;
	}

	private void executeTarget(final VBox box, final Provisioning.Target target) throws IOException, InterruptedException, TimeoutException, ExecutionException {
		LOGGER.info("executing target " + target.getName());
		for (Object o : target.getPortForwardOrAwaitPortOrAwaitState()) {
			LOGGER.debug("executing " + o);
			if (o instanceof Provisioning.Target.PortForward)
				portForward(box.getName(), (Provisioning.Target.PortForward) o);
			else if (o instanceof Provisioning.Target.KeyboardPutScanCodes)
				keyboardPutScanCodes(box.getName(), ((Provisioning.Target.KeyboardPutScanCodes) o));
			else if (o instanceof Provisioning.Target.Sleep) {
				final Provisioning.Target.Sleep s = (Provisioning.Target.Sleep) o;
				final long seconds = s.getMinutes() * 60 + s.getSeconds();

				LOGGER.info("sleeping for " + seconds + " second(s)");
				Thread.sleep(seconds * 1000);
			} else if (o instanceof Provisioning.Target.Exec) {
				try {
					ExecUtils.exec(new CSVReader(new StringReader(subst(((Provisioning.Target.Exec) o).getValue())), ' ').readNext());
				} catch (ExecutionException e) {
					if (((Provisioning.Target.Exec) o).isFailonerror())
						throw e;
					else
						LOGGER.info("ignoring error " + e.getMessage());
				}
			} else if (o instanceof Provisioning.Target.AwaitPort) {
				awaitPort((Provisioning.Target.AwaitPort) o);
			} else if (o instanceof Provisioning.Target.AwaitState) {
				box.awaitState( DurationUtils.millisForString(((Provisioning.Target.AwaitState) o).getTimeout()), ((Provisioning.Target.AwaitState) o).getState());
			} else
				throw new AssertionError("unexpected provision");
		}

		// snapshots are expensive in terms of disk space
		// box.takeSnapshot(Snapshot.valueOf("post-provision-" + target.getName()));
	}

	private void awaitPort(final Provisioning.Target.AwaitPort ap) throws IOException, TimeoutException, InterruptedException {
		awaitPort(ap.getHost(), ap.getPort(), ap.getTimeout());
	}

	public static void awaitPort(String host, int port, String timeout) throws IOException, TimeoutException, InterruptedException {
		final long start = System.currentTimeMillis();
		final String desc = host + ":" + port;
		while (true) {
			final long remaining = start + DurationUtils.millisForString(timeout) - System.currentTimeMillis();
			LOGGER.info("awaiting " + desc + " for " + DurationUtils.prettyPrint(remaining));
			try {
				new Socket(host, port).close();
				LOGGER.info("port available");
				return;
			} catch (ConnectException e) {
				// nop
			}

			if (remaining < 0) {
				throw new TimeoutException("timed out waiting for " + desc);
			}

			Thread.sleep(Math.min(10000l, remaining));
		}
	}

	void stopServer() throws Exception {
		if (server.isRunning()) {
			LOGGER.info("stopping local web server");
			server.stop();
		}
	}

	void startServer(final VBox box) throws Exception {
		LOGGER.info("starting local web server on port " + getServerPort());

		final ResourceHandler rh = new ResourceHandler();
		rh.setBaseResource(Resource.newResource(box.getSrc().toURL()));
		LOGGER.info("serving " + rh.getResourceBase());
		server.setHandler(rh);
		server.start();

		final URL u = new URL("http://" + InetAddress.getLocalHost().getHostAddress() + ":" + getServerPort() + "/VirtualBox.xml");
		LOGGER.info("testing server by getting " + u);
		final HttpURLConnection c = (HttpURLConnection) u.openConnection();
		c.connect();
		if (200 != c.getResponseCode()) throw new IllegalStateException(c.getResponseCode() + " " +c.getResponseMessage());
		c.disconnect();

	}

	private static int findFreePort() throws IOException {
		final ServerSocket server = new ServerSocket(0);
		final int port = server.getLocalPort();
		server.close();
		return port;
	}

	public void keyboardPutScanCodes(String name, Provisioning.Target.KeyboardPutScanCodes ksc) throws IOException, InterruptedException, ExecutionException {

		{
			final String keys = ksc.getKeys();
			if (keys != null) {
				LOGGER.info("typing keys " + keys);
				final List<Integer> sc = new ArrayList<Integer>();
				for (String key : keys.split(",")) {
					for (int c : ScanCodes.forKey(key)) {
						sc.add(c);
					}
				}
				keyboardPutScanCodes(name, ArrayUtils.toPrimitive(sc.toArray(new Integer[sc.size()])));
			}
		}
		{
			String line;
			line = ksc.getLine();
			if (line != null) {
				line = subst(line);

				LOGGER.info("typing line '" + line + "'");

				keyboardPutScanCodes(name, ArrayUtils.addAll(ScanCodes.forString(line), ScanCodes.forKey("Enter")));
			}
		}

		{
			String text = ksc.getValue();
			if (text != null && text.length() > 0) {
				text = subst(text);

				LOGGER.info("typing text '" + text + "'");

				keyboardPutScanCodes(name, ArrayUtils.addAll(ScanCodes.forString(text), ScanCodes.forKey("Enter")));
			}
		}
	}

	@Override
	public String subst(String line) throws IOException, InterruptedException, ExecutionException {
		line = line.replaceAll("\\$\\{server\\.ip\\}", InetAddress.getLocalHost().getHostAddress());
		line = line.replaceAll("\\$\\{server\\.port\\}", String.valueOf(getServerPort()));
		return super.subst(line);
	}

	private void keyboardPutScanCodes(String name, int[] scancodes) throws IOException, InterruptedException, ExecutionException {
		LOGGER.debug("typing " + Arrays.toString(scancodes));

		while (scancodes.length > 0) {
			final List<String> command = new ArrayList<String>();
			command.addAll(Arrays.asList("vboxmanage", "controlvm", name, "keyboardputscancode"));

			int i = 0;
			for (int scancode : scancodes) {
				command.add((scancode > 0xf ? "" : "0") + Integer.toHexString(scancode));
				i++;
				// split on enter
				if (i >= 16 || scancode == 156) {
					break;
				}
			}
			ExecUtils.exec(command.toArray(new String[command.size()]));
			Thread.sleep(scancodes[i - 1] == 156 ? 2000 : 100); //  a short sleep to let the OS digest
			scancodes = ArrayUtils.subarray(scancodes, i, scancodes.length);
		}
	}

	private void portForward(String name, Provisioning.Target.PortForward pf) throws IOException, InterruptedException, ExecutionException {
		final int hostPort = pf.getHostport();
		final int guestPort = pf.getGuestport();
		LOGGER.info("adding port forward hostport=" + hostPort + " guestport=" + guestPort);
		ExecUtils.exec("vboxmanage", "setextradata", name, "VBoxInternal/Devices/e1000/0/LUN#0/Config/" + guestPort + "/HostPort", String.valueOf(hostPort));
		ExecUtils.exec("vboxmanage", "setextradata", name, "VBoxInternal/Devices/e1000/0/LUN#0/Config/" + guestPort + "/GuestPort", String.valueOf(guestPort));
		ExecUtils.exec("vboxmanage", "setextradata", name, "VBoxInternal/Devices/e1000/0/LUN#0/Config/" + guestPort + "/Protocol", "TCP");
	}

	public int getServerPort() {
		return server.getConnectors()[0].getPort();
	}
}