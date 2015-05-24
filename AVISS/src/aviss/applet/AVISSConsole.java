package aviss.applet;

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Composite;
import org.mihalis.opal.switchButton.SwitchButton;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Group;
import org.eclipse.wb.swt.SWTResourceManager;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Text;
import org.mihalis.opal.itemSelector.DLItem;
import org.mihalis.opal.itemSelector.DualList;
import org.mihalis.opal.itemSelector.SelectionChangeEvent;
import org.mihalis.opal.itemSelector.SelectionChangeListener;

import aviss.audio.AudioManager;
import aviss.data.AVGeneratorType;

public class AVISSConsole implements Runnable {

	protected Shell shell;
	private Text audioFilePath_textBox;
	private AVISSApplet pApp;
	private AudioManager aMan;

	/**
	 * Launch the application.
	 * 
	 * @param args
	 * 
	 *            public static void main(String[] args) { try { //new
	 *            AVSConsole(); } catch (Exception e) { e.printStackTrace(); } }
	 */
	public AVISSConsole() {

	}

	public AVISSConsole(AVISSApplet applet, AudioManager am) {
		pApp = applet;
		aMan = am;
	}

	@Override
	public void run() {
		Display display = Display.getDefault();
		shell = new Shell(display);
		createContents();
		shell.open();
		shell.layout();
		shell.pack();

		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		display.dispose();
	}

	/**
	 * Create contents of the window.
	 */
	protected void createContents() {
		shell.setSize(918, 653);
		shell.setText("AVS Console");
		shell.setLayout(null);

		Composite composite = new Composite(shell, SWT.NONE);
		composite.setLayout(null);
		composite.setBounds(10, 10, 880, 586);

		Group grpFftSettings = new Group(composite, SWT.NONE);
		grpFftSettings.setFont(SWTResourceManager.getFont("Segoe UI", 9,
				SWT.BOLD));
		grpFftSettings.setText("FFT Settings");
		grpFftSettings.setBounds(10, 213, 349, 353);

		Label label = new Label(grpFftSettings, SWT.NONE);
		label.setText("FFT Size");
		label.setBounds(10, 36, 70, 20);

		Label label_1 = new Label(grpFftSettings, SWT.WRAP);
		label_1.setText("FFT Minimum Octave Size");
		label_1.setBounds(10, 76, 94, 48);

		Label label_2 = new Label(grpFftSettings, SWT.WRAP);
		label_2.setText("# of FFT Octave Bands");
		label_2.setBounds(10, 130, 94, 48);

		Label label_3 = new Label(grpFftSettings, SWT.NONE);
		label_3.setText("Resultant Number of FFT Bands = ");
		label_3.setBounds(10, 196, 237, 20);

		Label fftNumBands_label = new Label(grpFftSettings, SWT.NONE);
		fftNumBands_label.setText("");
		fftNumBands_label.setBounds(250, 196, 70, 20);

		Scale fftMinOctaveSize_slider = new Scale(grpFftSettings, SWT.NONE);
		fftMinOctaveSize_slider.setBounds(110, 76, 210, 48);
		fftMinOctaveSize_slider.setSelection(aMan.fftMinOctaveSize);
		fftMinOctaveSize_slider.setMinimum(4);
		fftMinOctaveSize_slider.setMaximum(400);
		fftMinOctaveSize_slider.addListener(SWT.MouseUp, new Listener() {
			public void handleEvent(Event event) {
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						if (pApp.aquireGeneratorLock()) {
							if (aMan.aquireFFTDataLock()) {
								aMan.setFFTMinOctaveSize(fftMinOctaveSize_slider
										.getSelection());
								pApp.reinitAVGenerators();
								aMan.releaseFFTDataLock();
							}
							pApp.releaseGeneratorLock();
						}
					}
				});
				fftNumBands_label.setText(Integer.toString(aMan
						.getFFTAvgSpecSize()));
			}
		});

		Scale fftNumOctaveBands_slider = new Scale(grpFftSettings, SWT.NONE);
		fftNumOctaveBands_slider.setBounds(110, 130, 210, 48);
		fftNumOctaveBands_slider.setMinimum(4);
		fftNumOctaveBands_slider.setSelection(aMan.fftNumOctaveBands);
		fftNumOctaveBands_slider.addListener(SWT.MouseUp, new Listener() {
			public void handleEvent(Event event) {
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						if (pApp.aquireGeneratorLock()) {
							if (aMan.aquireFFTDataLock()) {
								aMan.setFFTNumOctaveBands(fftNumOctaveBands_slider
										.getSelection());
								pApp.reinitAVGenerators();
								aMan.releaseFFTDataLock();
							}
							pApp.releaseGeneratorLock();
						}
					}
				});
				fftNumBands_label.setText(Integer.toString(aMan
						.getFFTAvgSpecSize()));
			}
		});

		Combo fftAverageType_comboBox = new Combo(grpFftSettings, SWT.NONE);
		fftAverageType_comboBox
				.setItems(new String[] { "Logarithmic", "Linear" });
		fftAverageType_comboBox.select(0);
		fftAverageType_comboBox.setBounds(147, 278, 173, 28);
		fftAverageType_comboBox.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {

				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						if (pApp.aquireGeneratorLock()) {
							if (aMan.aquireFFTDataLock()) {
								aMan.setFFTAverageType(fftAverageType_comboBox
										.getText());
								pApp.reinitAVGenerators();
								aMan.releaseFFTDataLock();
							}
							pApp.releaseGeneratorLock();
						}
					}
				});
				fftNumBands_label.setText(Integer.toString(aMan
						.getFFTAvgSpecSize()));
			}
		});

		Combo fftWinType_comboBox = new Combo(grpFftSettings, SWT.NONE);
		fftWinType_comboBox.setItems(new String[] { "None", "Bartlett",
				"Bartlett-Hann", "Blackman", "Cosine", "Gauss", "Hamming",
				"Hann", "Lanczos", "Triangular" });
		fftWinType_comboBox.select(0);
		fftWinType_comboBox.setBounds(147, 234, 173, 28);
		fftWinType_comboBox.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {

				aMan.setFFTWindowType(fftWinType_comboBox.getText());
			}
		});
		
		Combo fftSize_comboBox = new Combo(grpFftSettings, SWT.NONE);
		fftSize_comboBox.setBounds(110, 35, 210, 48);
		fftSize_comboBox
				.setItems(new String[] { "256", "512", "1024", "2048" });
		fftSize_comboBox.select(2);
		fftSize_comboBox.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						if (pApp.aquireGeneratorLock()) {
							if (aMan.aquireFFTDataLock()) {
								aMan.setFFTSize(Integer.parseInt(fftSize_comboBox.getText()), 
										fftWinType_comboBox.getText(), 
										fftAverageType_comboBox.getText());
								pApp.reinitAVGenerators();
								aMan.releaseFFTDataLock();
							}
							pApp.releaseGeneratorLock();
						}
					}
				});
				fftNumBands_label.setText(Integer.toString(aMan
						.getFFTAvgSpecSize()));
			}
		});

		Label label_5 = new Label(grpFftSettings, SWT.WRAP);
		label_5.setText("FFT Window Type");
		label_5.setBounds(10, 237, 131, 28);

		Label label_6 = new Label(grpFftSettings, SWT.WRAP);
		label_6.setText("FFT Average Type");
		label_6.setBounds(10, 281, 131, 28);

		Group grpMicrophoneInput = new Group(composite, SWT.NONE);
		grpMicrophoneInput.setFont(SWTResourceManager.getFont("Segoe UI", 9,
				SWT.BOLD));
		grpMicrophoneInput.setText("Audio I/O Settings");
		grpMicrophoneInput.setBounds(10, 0, 349, 205);

		Scale gain_slider = new Scale(grpMicrophoneInput, SWT.NONE);
		gain_slider.setBounds(104, 77, 235, 48);
		gain_slider.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				aMan.setInputGain((float) gain_slider.getSelection() / 100f);
			}
		});

		Scale volume_slider = new Scale(grpMicrophoneInput, SWT.NONE);
		volume_slider.setBounds(104, 139, 235, 48);
		volume_slider.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				aMan.setPlayerVolume((float) volume_slider.getSelection() / 10f);
			}
		});

		Group grpFileInputSettings = new Group(composite, SWT.NONE);
		grpFileInputSettings.setFont(SWTResourceManager.getFont("Segoe UI", 9,
				SWT.BOLD));
		grpFileInputSettings.setText("File Input Settings");
		grpFileInputSettings.setBounds(368, 0, 502, 205);

		audioFilePath_textBox = new Text(grpFileInputSettings, SWT.BORDER);
		audioFilePath_textBox.setBounds(10, 49, 415, 26);

		Button audioFileBrowse_button = new Button(grpFileInputSettings,
				SWT.NONE);
		audioFileBrowse_button.setBounds(432, 47, 60, 30);
		audioFileBrowse_button.setText("Browse");
		audioFileBrowse_button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog dlg = new FileDialog(shell, SWT.MULTI);

				dlg.setFilterExtensions(new String[] { "*.wav", "*.mp3",
						"*.aif" });
				String fn = dlg.open();
				if (fn != null) {
					audioFilePath_textBox.setText(fn);
				}
			}
		});

		Label lblAudioFilePath = new Label(grpFileInputSettings, SWT.NONE);
		lblAudioFilePath.setBounds(10, 27, 132, 20);
		lblAudioFilePath.setText("Audio FIle Path");

		Label loadAudioFileFeedback_label = new Label(grpFileInputSettings,
				SWT.NONE);
		loadAudioFileFeedback_label.setBounds(116, 81, 376, 26);
		loadAudioFileFeedback_label.setText("");

		Button loadAudioFile_button = new Button(grpFileInputSettings, SWT.NONE);
		loadAudioFile_button.setBounds(10, 81, 90, 30);
		loadAudioFile_button.setText("Load File");
		loadAudioFile_button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				File f = new File(audioFilePath_textBox.getText());
				if (f.exists() && !f.isDirectory()) {
					boolean loadSuccess = aMan.setPlaybackAudioFile(
							audioFilePath_textBox.getText(), false);
					if (loadSuccess) {
						loadAudioFileFeedback_label
								.setText("File loaded successfully!");
						loadAudioFileFeedback_label
								.setForeground(SWTResourceManager
										.getColor(SWT.COLOR_GREEN));
					} else {
						loadAudioFileFeedback_label
								.setText("File unable to load.");
						loadAudioFileFeedback_label
								.setForeground(SWTResourceManager
										.getColor(SWT.COLOR_RED));
					}
				}
			}
		});

		Button play_button = new Button(grpFileInputSettings, SWT.NONE);
		play_button.setBounds(76, 165, 60, 30);
		play_button.setText("Play");
		play_button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				aMan.playPlayer();
			}
		});

		Button stop_button = new Button(grpFileInputSettings, SWT.NONE);
		stop_button.setBounds(142, 165, 60, 30);
		stop_button.setText("Stop");
		stop_button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				aMan.pausePlayer();
			}
		});

		Button rewind_button = new Button(grpFileInputSettings, SWT.NONE);
		rewind_button.setText("Rewind");
		rewind_button.setBounds(10, 165, 60, 30);
		rewind_button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				aMan.seekPlayer(0);
			}
		});

		SwitchButton micEnable_switchButton = new SwitchButton(
				grpMicrophoneInput, SWT.NONE);
		micEnable_switchButton.setText("Microphone Input");
		micEnable_switchButton.setBounds(10, 34, 200, 36);
		micEnable_switchButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(final SelectionEvent e) {
				if (micEnable_switchButton.getSelection()) {
					aMan.enableAudioIn();
					gain_slider.setEnabled(true);
					grpFileInputSettings.setEnabled(false);
				} else {
					aMan.startAudioFilePlayback(true);
					gain_slider.setEnabled(false);
					grpFileInputSettings.setEnabled(true);
				}
			}

			@Override
			public void widgetDefaultSelected(final SelectionEvent e) {
			}
		});

		Label label_7 = new Label(grpMicrophoneInput, SWT.NONE);
		label_7.setText("Input Gain");
		label_7.setBounds(10, 93, 70, 20);

		Label lblOutputGain = new Label(grpMicrophoneInput, SWT.NONE);
		lblOutputGain.setText("Output Gain");
		lblOutputGain.setBounds(6, 151, 92, 20);

		DualList generator_dualList = new DualList(composite, SWT.NONE);
		generator_dualList.setBounds(365, 239, 505, 337);
		for (AVGeneratorType genName : AVGeneratorType.values()) {
			generator_dualList.add(new DLItem(genName.toString().replace('_',
					' ')));
		}

		generator_dualList
				.addSelectionChangeListener(new SelectionChangeListener() {
					@Override
					public void widgetSelected(final SelectionChangeEvent e) {
						for (final DLItem item : e.getItems()) {
							if (item.getLastAction() == DLItem.LAST_ACTION.SELECTION) {
								pApp.addAVGenerator(AVGeneratorType
										.valueOf(item.getText().replace(' ',
												'_')));
							} else {
								pApp.removeAVGenerator(item.getText().replace(
										' ', '_'));
							}
						}
					}
				});

		Label lblAvailableVgens = new Label(composite, SWT.NONE);
		lblAvailableVgens.setFont(SWTResourceManager.getFont("Segoe UI", 9,
				SWT.BOLD));
		lblAvailableVgens.setBounds(408, 219, 135, 20);
		lblAvailableVgens.setText("Available vGens");

		Label lblActiveVgens = new Label(composite, SWT.NONE);
		lblActiveVgens.setFont(SWTResourceManager.getFont("Segoe UI", 9,
				SWT.BOLD));
		lblActiveVgens.setText("Active vGens");
		lblActiveVgens.setBounds(668, 219, 94, 20);
	}
}
