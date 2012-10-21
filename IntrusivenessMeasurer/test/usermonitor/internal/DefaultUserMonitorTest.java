package usermonitor.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import usermonitor.MemoryInfo;

import commons.test.LoggedTest;

public class DefaultUserMonitorTest extends LoggedTest {
	
	private final double testDeltaError = 0.005;
	
	private final String testMemoryFileName = "memory";
	private final String testCPUInfoFileName = "cpuInfo";
	private final String testCPUUsageFileName = "cpuUsage";
	private final String testMemoryRealFileName = "/proc/meminfo";
	private final String testCPUInfoRealFileName = "/proc/cpuinfo";
	private final String testCPUUsageRealUsageFileName = "/proc/stat";

	private final double testTotalMemory = 1000;
	private final double testUsedMemory = 600;
	
	private final int performanceTestNumberOfRepetitions = 1000;
	private final double performanceTestMemoryLimitTime = 100;
			
	private DefaultUserMonitor monitor;
	
	@Before
	public void setUp() throws IOException {
		new File(testMemoryFileName).createNewFile();
		new File(testCPUInfoFileName).createNewFile();
		new File(testCPUUsageFileName).createNewFile();
		
		monitor = new DefaultUserMonitor(testMemoryFileName, testCPUInfoFileName, testCPUUsageFileName);
	}
	
	@After
	public void tearDown() {
		new File(testMemoryFileName).delete();
		new File(testCPUInfoFileName).delete();
		new File(testCPUUsageFileName).delete();
	}
	
	@Test(expected = FileNotFoundException.class)
	public void testConstructorMustReceiveExistentMemoryInfoFileName() throws FileNotFoundException {
		new DefaultUserMonitor("non-existent file", testCPUInfoFileName, testCPUUsageFileName);
	}
	
	@Test(expected = FileNotFoundException.class)
	public void testConstructorMustReceiveExistentCPUInfoFileName() throws FileNotFoundException {
		new DefaultUserMonitor(testMemoryFileName, "non-existent file", testCPUUsageFileName);
	}
	
	@Test(expected = FileNotFoundException.class)
	public void testConstructorMustReceiveExistentCPUUsageFileName() throws FileNotFoundException {
		new DefaultUserMonitor(testMemoryFileName, testCPUInfoFileName, "non-existent file");
	}
	
	@Test
	public void testGetMemoryUsage() throws IOException {
		writeValidMemoryFile();
		MemoryInfo result = monitor.getMemoryUsage();
		assertEquals(testTotalMemory, result.getTotalMemory(), testDeltaError);
		assertEquals(testUsedMemory, result.getUsedMemory(), testDeltaError);
	}
	
	@Test
	public void testGetMemoryUsageFromFileWithInvertedOrderedData() throws IOException {
		writeMemoryFileWithInvertedOrderedData();
		MemoryInfo result = monitor.getMemoryUsage();
		assertEquals(testTotalMemory, result.getTotalMemory(), testDeltaError);
		assertEquals(testUsedMemory, result.getUsedMemory(), testDeltaError);
	}
	
	@Test
	public void testGetMemoryUsageFromFileWithCommentedLines() throws IOException {
		writeMemoryFileCommentedLines();
		MemoryInfo result = monitor.getMemoryUsage();
		assertEquals(testTotalMemory, result.getTotalMemory(), testDeltaError);
		assertEquals(testUsedMemory, result.getUsedMemory(), testDeltaError);
	}
	
	@Test(expected = IOException.class)
	public void testGetMemoryUsageFromIncompleteFile() throws IOException {
		writeMemoryFileWithMissingInformation();
		monitor.getMemoryUsage();
	}
	
	@Test(expected = IOException.class)
	public void testGetMemoryUsageFromInvalidFormatFile() throws IOException {
		writeMemoryFileWithInvalidFormat();
		monitor.getMemoryUsage();
	}
	
	@Test(expected = IOException.class)
	public void testGetMemoryUsageFromEmptyFile() throws IOException {
		monitor.getMemoryUsage();
	}
	
	@Test
	public void testGetMemoryPerformanceTest() throws IOException {
		writeValidMemoryFile();
		long timeStart = System.currentTimeMillis();
		
		for (int i = 0; i < performanceTestNumberOfRepetitions; i++) {
			monitor.getMemoryUsage();
		}
		
		double delta = System.currentTimeMillis() - timeStart;
		assertTrue(delta/performanceTestNumberOfRepetitions < performanceTestMemoryLimitTime);
	}
	
	@Test
	public void testGetMemoryFromRealFilePerformanceTest() throws IOException {
		monitor = new DefaultUserMonitor(testMemoryRealFileName, testCPUInfoFileName, testCPUUsageFileName);
		long timeStart = System.currentTimeMillis();
		
		for (int i = 0; i < performanceTestNumberOfRepetitions; i++) {
			monitor.getMemoryUsage();
		}
		
		double delta = System.currentTimeMillis() - timeStart;
		assertTrue(delta/performanceTestNumberOfRepetitions < performanceTestMemoryLimitTime);
	}
	
	private void writeValidMemoryFile() throws IOException {
		RandomAccessFile fileMemory = new RandomAccessFile(testMemoryFileName, "rw");
		
		fileMemory.write("# Some header\n".getBytes());
		fileMemory.write("info1     nothing    \n".getBytes());
		fileMemory.write(("MemTotal:    " + testTotalMemory + "\n").getBytes());
		fileMemory.write("info2     nothing    \n".getBytes());
		fileMemory.write(("MemFree:    " + (testTotalMemory - testUsedMemory) + "\n").getBytes());
		fileMemory.write("info3    nothing    \n".getBytes());
		
		fileMemory.close();
	}
	
	private void writeMemoryFileWithMissingInformation() throws IOException {
		RandomAccessFile fileMemory = new RandomAccessFile(testMemoryFileName, "rw");
		
		fileMemory.write("# Some header\n".getBytes());
		fileMemory.write("info1     nothing    \n".getBytes());
		fileMemory.write(("MemTotal:    " + testTotalMemory + "\n").getBytes());
		fileMemory.write("info2     nothing    \n".getBytes());
		fileMemory.write("info3    nothing    \n".getBytes());
		
		fileMemory.close();
	}
	
	private void writeMemoryFileWithInvalidFormat() throws IOException {
		RandomAccessFile fileMemory = new RandomAccessFile(testMemoryFileName, "rw");
		
		fileMemory.write("# Some header\n".getBytes());
		fileMemory.write("info1     nothing    \n".getBytes());
		fileMemory.write((testTotalMemory + "\n").getBytes());
		fileMemory.write("info2     nothing    \n".getBytes());
		fileMemory.write(("MemFree:    " + (testTotalMemory - testUsedMemory) + "\n").getBytes());
		fileMemory.write("info3    nothing    \n".getBytes());
		
		fileMemory.close();
	}
	
	private void writeMemoryFileCommentedLines() throws IOException {
		RandomAccessFile fileMemory = new RandomAccessFile(testMemoryFileName, "rw");
		
		fileMemory.write("# Some header\n".getBytes());
		fileMemory.write("info1     nothing    \n".getBytes());
		fileMemory.write("# comment\n".getBytes());
		fileMemory.write(("MemTotal:    " + testTotalMemory + "\n").getBytes());
		fileMemory.write("info2     nothing    \n".getBytes());
		fileMemory.write("# comment\n".getBytes());
		fileMemory.write(("MemFree:    " + (testTotalMemory - testUsedMemory) + "\n").getBytes());
		fileMemory.write("info3    nothing    \n".getBytes());
		
		fileMemory.close();
	}
	
	private void writeMemoryFileWithInvertedOrderedData() throws IOException {
		RandomAccessFile fileMemory = new RandomAccessFile(testMemoryFileName, "rw");
		
		fileMemory.write("# Some header\n".getBytes());
		fileMemory.write("info1     nothing    \n".getBytes());
		fileMemory.write(("MemFree:    " + (testTotalMemory - testUsedMemory) + "\n").getBytes());
		fileMemory.write("info2     nothing    \n".getBytes());
		fileMemory.write(("MemTotal:    " + testTotalMemory + "\n").getBytes());
		fileMemory.write("info3    nothing    \n".getBytes());
		
		fileMemory.close();
	}
}
