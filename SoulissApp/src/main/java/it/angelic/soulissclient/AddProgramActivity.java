package it.angelic.soulissclient;

import it.angelic.soulissclient.db.SoulissCommandDTO;
import it.angelic.soulissclient.db.SoulissDBHelper;
import it.angelic.soulissclient.db.SoulissTriggerDTO;
import it.angelic.soulissclient.helpers.AlertDialogHelper;
import it.angelic.soulissclient.helpers.SoulissPreferenceHelper;
import it.angelic.soulissclient.model.ISoulissExecutable;
import it.angelic.soulissclient.model.SoulissCommand;
import it.angelic.soulissclient.model.SoulissNode;
import it.angelic.soulissclient.model.SoulissScene;
import it.angelic.soulissclient.model.SoulissTypical;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ToggleButton;


public class AddProgramActivity extends Activity {
    //arrays per spinner
    private SoulissNode[] nodiArray;
    private SoulissNode[] nodiArrayWithExtra;

    private SoulissDBHelper datasource = new SoulissDBHelper(this);
    private SoulissPreferenceHelper opzioni;
    private TextView tvcommand;
    private SoulissCommand collected;
    private Spinner outputNodeSpinner;
    private Spinner outputTypicalSpinner;
    private RadioButton radioTimed;
    private RadioButton radioPositional;
    private RadioButton radioTrigger;
    private ToggleButton togglehomeaway;
    private LinkedList<SoulissScene> scenes;
    private Spinner outputCommandSpinner;
    private TimePicker commandTimePicker;
    private CheckBox checkboxRecursive;
    private int[] spinnerArrVal;
    private Spinner commandSpinnerInterval;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        opzioni = new SoulissPreferenceHelper(this.getApplicationContext());
        // tema
        if (opzioni.isLightThemeSelected())
            setTheme(R.style.LightThemeSelector);
        else
            setTheme(R.style.DarkThemeSelector);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_addprogram);

        if (!opzioni.isDbConfigured()) {
            AlertDialogHelper.dbNotInitedDialog(this);
        }

        datasource = new SoulissDBHelper(this);
        datasource.open();
        spinnerArrVal = getResources().getIntArray(R.array.scheduleIntervalValues);
        ImageView nodeic = (ImageView) findViewById(R.id.timed_icon);
        tvcommand = (TextView) findViewById(R.id.textViewCommand);
        nodeic.setColorFilter(getResources().getColor(R.color.aa_violet), android.graphics.PorterDuff.Mode.SRC_ATOP);

        ImageView nodeic2 = (ImageView) findViewById(R.id.position_icon);
        nodeic2.setColorFilter(getResources().getColor(R.color.aa_blue), android.graphics.PorterDuff.Mode.SRC_ATOP);

        ImageView nodeic3 = (ImageView) findViewById(R.id.triggered_icon);
        nodeic3.setColorFilter(getResources().getColor(R.color.aa_red), android.graphics.PorterDuff.Mode.SRC_ATOP);

        // prendo tipici dal DB
        List<SoulissNode> goer = datasource.getAllNodes();
        nodiArray = new SoulissNode[goer.size()];
        nodiArray = goer.toArray(nodiArray);
        //Aggiungo massivo
        SoulissNode massive = new SoulissNode((short) Constants.COMMAND_MASSIVE);// MASSIVO
        massive.setName(getString(R.string.allnodes));
        massive.setTypicals(datasource.getUniqueTypicals(massive));
        goer.add(massive);
        //AGGIUNGO scene
        SoulissNode fake = new SoulissNode((short) Constants.COMMAND_FAKE_SCENE);// MASSIVO
        fake.setName(getString(R.string.scenes_title));
        goer.add(fake);

        nodiArrayWithExtra = new SoulissNode[goer.size()];
        nodiArrayWithExtra = goer.toArray(nodiArrayWithExtra);
        SoulissClient.setBackground((ScrollView) findViewById(R.id.ScrollView01), getWindowManager());

    }

    /**
     * Carica gli spinner ed i widget ai valori salvati in precedenza
     */
    private void setFields() {
        if (collected == null)
            return;
        else if (collected.getCommandDTO().getNodeId() == Constants.COMMAND_FAKE_SCENE) {
            //get last
            outputNodeSpinner.setSelection(outputNodeSpinner.getAdapter().getCount() - 1);
            //tipici sono scene
            int sceneid = collected.getCommandDTO().getSlot();
            Log.w(Constants.TAG, "Restoring scene ID" + sceneid);
            setTypicalSpinner(outputTypicalSpinner, nodiArrayWithExtra[nodiArrayWithExtra.length - 1]);
            int tot = outputTypicalSpinner.getCount();
            for (int i = 0; i < tot; i++) {
                SoulissScene now = (SoulissScene) outputTypicalSpinner.getItemAtPosition(i);
                if (now.getId() == sceneid) {
                    //seleziono la scena da eseguire
                    outputTypicalSpinner.setSelection(i);
                    Log.i(Constants.TAG, "SELECTED:" + i);
                }
            }
            tvcommand.setVisibility(View.INVISIBLE);
            //   outputTypicalSpinner.setSelection(sceneid);
        } else if (collected.getCommandDTO().getNodeId() == Constants.MASSIVE_NODE_ID) {
            //get last but one
            outputNodeSpinner.setSelection(outputNodeSpinner.getAdapter().getCount() - 2);
            //TODO selezione comando
        } else {//reload nodo, tipico e comando
            outputNodeSpinner.setSelection(collected.getCommandDTO().getNodeId());
            setTypicalSpinner(outputTypicalSpinner, nodiArrayWithExtra[collected.getCommandDTO().getNodeId()]);
            int tot = outputTypicalSpinner.getCount();
            for (int i = 0; i < tot; i++) {
                SoulissTypical now = (SoulissTypical) outputTypicalSpinner.getItemAtPosition(i);
                if (now.getTypicalDTO().getSlot() == collected.getCommandDTO().getSlot()) {
                    //seleziono la scena da eseguire
                    outputTypicalSpinner.setSelection(i);
                    Log.i(Constants.TAG, "SELECTED TYP:" + i);
                    fillCommandSpinner(outputCommandSpinner, now);
                    break;
                }
            }
            tot = outputCommandSpinner.getCount();
            for (int t = 0; t < tot; t++) {
                SoulissCommand now = (SoulissCommand) outputCommandSpinner.getItemAtPosition(t);
                if (now.getCommandDTO().getCommand() == collected.getCommandDTO().getCommand()) {
                    //seleziono la scena da eseguire
                    outputCommandSpinner.setSelection(t);
                    Log.i(Constants.TAG, "SELECTED COMMAND POS:" + t);
                    break;
                }
            }

        }
        //SETTA TYPE
        if (collected.getType() == Constants.COMMAND_TIMED) {
            Calendar sched = collected.getCommandDTO().getScheduledTime();
            commandTimePicker.setCurrentHour(sched.get(Calendar.HOUR_OF_DAY));
            commandTimePicker.setCurrentMinute(sched.get(Calendar.MINUTE));

            if (collected.getCommandDTO().getInterval() > 0) {
                int j = 0;
                for (int spi : spinnerArrVal) {
                    if (collected.getCommandDTO().getInterval() == spi)
                        commandSpinnerInterval.setSelection(j);
                    j++;
                }
                checkboxRecursive.setChecked(true);
            }

        } else if (collected.getType() == Constants.COMMAND_COMEBACK_CODE || collected.getType() == Constants.COMMAND_GOAWAY_CODE) {
            radioPositional.performClick();
            togglehomeaway.setChecked(collected.getType() == Constants.COMMAND_COMEBACK_CODE);
        } else if (collected.getType() == Constants.COMMAND_TRIGGERED) {
            radioTrigger.performClick();
            //TODO carica trigger
            /*SoulissTypical trig = ((SoulissTypical) triggeredTypicalSpinner.getSelectedItem());
                    // campi trigger
                    SoulissTriggerDTO trigger = new SoulissTriggerDTO();
                    trigger.setInputNodeId(trig.getTypicalDTO().getNodeId());

                    trigger.setInputSlot(trig.getTypicalDTO().getSlot());
                    trigger.setOp(buttComparator.getText().toString());
                    trigger.setCommandId(programToSave.getCommandDTO().getCommandId());
                    trigger.setThreshVal(Integer.parseInt(et.getText().toString()));

                    trigger.persist(datasource);*/
        }
    }

    @SuppressLint({"NewApi", "NewApi"})
    @Override
    protected void onStart() {
        super.onStart();
        scenes = datasource.getScenes(this);
        outputNodeSpinner = (Spinner) findViewById(R.id.spinner2);
        fillNodeSpinnerWithExtra(outputNodeSpinner);
        outputTypicalSpinner = (Spinner) findViewById(R.id.spinner3);
        outputCommandSpinner = (Spinner) findViewById(R.id.spinnerCommand);

        radioTimed = (RadioButton) findViewById(R.id.RadioButtonTime);
        radioPositional = (RadioButton) findViewById(R.id.RadioButtonPosition);
        radioTrigger = (RadioButton) findViewById(R.id.RadioButtonTriggered);
        // time based
        final TextView textviewTimed = (TextView) findViewById(R.id.textviewtimed);
        checkboxRecursive = (CheckBox) findViewById(R.id.checkBoxRecursive);
        commandTimePicker = (TimePicker) findViewById(R.id.timePicker);
        commandSpinnerInterval = (Spinner) findViewById(R.id.spinnerRepeatEvery);
        // position based
        final TextView textviewPositional = (TextView) findViewById(R.id.textViewInfoPos);
        togglehomeaway = (ToggleButton) findViewById(R.id.toggleButtonPosition);
        // data based
        final TextView textviewTriggered = (TextView) findViewById(R.id.textViewInfoTrig);
        final Spinner triggeredNodeSpinner = (Spinner) findViewById(R.id.Spinner05);
        final Spinner triggeredTypicalSpinner = (Spinner) findViewById(R.id.Spinner06);
        fillNodeSpinner(triggeredNodeSpinner);
        final Button buttComparator = (Button) findViewById(R.id.buttonComparator);
        final EditText et = (EditText) findViewById(R.id.editTextThreshold);
        final Button btCancel = (Button) findViewById(R.id.buttonInsertProgram);
        final Button btSave = (Button) findViewById(R.id.buttonCancelProgram);


        /**
         * LISTENER SPINNER DESTINATARIO, IN TESTATA
         */
        final OnItemSelectedListener lit = new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                setTypicalSpinner(outputTypicalSpinner, nodiArrayWithExtra[pos]);
            }

            public void onNothingSelected(AdapterView<?> parent) {
            }
        };


        final OnItemSelectedListener lib = new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                // if (pos > 0) {
                SoulissNode mynode = nodiArrayWithExtra[(int) outputNodeSpinner.getSelectedItemId()];
                if (mynode.getId() > Constants.COMMAND_FAKE_SCENE) {
                    //le scene non hanno comandi
                    List<SoulissTypical> re = mynode
                            .getActiveTypicals(AddProgramActivity.this);

                    fillCommandSpinner(outputCommandSpinner, re.get(pos));
                } else {
                    fillSceneCommandSpinner(outputCommandSpinner, (SoulissScene) outputTypicalSpinner.getSelectedItem());
                }
            }

            public void onNothingSelected(AdapterView<?> parent) {
            }
        };

        // avoid auto call upon Creation with runnable
        outputTypicalSpinner.post(new Runnable() {
            public void run() {
                outputTypicalSpinner.setOnItemSelectedListener(lib);
                outputNodeSpinner.setOnItemSelectedListener(lit);
            }
        });

        /**
         * INTERLOCK della GUI
         */
        OnClickListener first_radio_listener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                radioPositional.setChecked(false);
                radioTrigger.setChecked(false);
                checkboxRecursive.setEnabled(true);
                commandTimePicker.setEnabled(true);
                commandSpinnerInterval.setEnabled(checkboxRecursive.isChecked());
                togglehomeaway.setEnabled(false);
                triggeredTypicalSpinner.setEnabled(false);
                triggeredNodeSpinner.setEnabled(false);
                buttComparator.setEnabled(false);
                et.setEnabled(false);
                textviewPositional.setEnabled(false);
                textviewTimed.setEnabled(true);
                textviewTriggered.setEnabled(false);
            }
        };
        radioTimed.setOnClickListener(first_radio_listener);

        OnClickListener se_radio_listener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                radioTimed.setChecked(false);
                radioTrigger.setChecked(false);
                checkboxRecursive.setEnabled(false);
                commandTimePicker.setEnabled(false);
                commandSpinnerInterval.setEnabled(false);
                togglehomeaway.setEnabled(true);
                triggeredTypicalSpinner.setEnabled(false);
                triggeredNodeSpinner.setEnabled(false);
                buttComparator.setEnabled(false);
                et.setEnabled(false);
                textviewPositional.setEnabled(true);
                textviewTimed.setEnabled(false);
                textviewTriggered.setEnabled(false);
            }
        };
        radioPositional.setOnClickListener(se_radio_listener);

        OnClickListener rw_radio_listener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                radioTimed.setChecked(false);
                radioPositional.setChecked(false);
                checkboxRecursive.setEnabled(false);
                commandTimePicker.setEnabled(false);
                commandSpinnerInterval.setEnabled(false);
                togglehomeaway.setEnabled(false);
                triggeredTypicalSpinner.setEnabled(true);
                triggeredNodeSpinner.setEnabled(true);
                buttComparator.setEnabled(true);
                et.setEnabled(true);
                textviewPositional.setEnabled(false);
                textviewTimed.setEnabled(false);
                textviewTriggered.setEnabled(true);
            }
        };
        radioTrigger.setOnClickListener(rw_radio_listener);
        // Check box ricorsivo, Interlock
        checkboxRecursive.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                commandSpinnerInterval.setEnabled(isChecked);
            }
        });
        /**
         * LISTENER SPINNER INPUT
         */
        OnItemSelectedListener lityp = new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                setTypicalSpinner(triggeredTypicalSpinner, nodiArrayWithExtra[pos]);
            }

            public void onNothingSelected(AdapterView<?> parent) {
            }
        };
        triggeredNodeSpinner.setOnItemSelectedListener(lityp);

        OnClickListener simplebttttt = new OnClickListener() {
            public void onClick(View v) {
                String old = buttComparator.getText().toString();
                if (">".compareTo(old) == 0) {
                    buttComparator.setText("=");
                } else if ("<".compareTo(old) == 0) {
                    buttComparator.setText(">");
                } else {
                    buttComparator.setText("<");
                }
            }
        };
        buttComparator.setOnClickListener(simplebttttt);

        OnClickListener saveProgramButtonListener = new OnClickListener() {
            public void onClick(View v) {

                ISoulissExecutable IToSave = (ISoulissExecutable) outputCommandSpinner.getSelectedItem();
                SoulissCommand programToSave = null;
                if (IToSave instanceof SoulissScene) {
                    SoulissScene toExec = (SoulissScene) IToSave;
                    SoulissCommandDTO dto = new SoulissCommandDTO();
                    dto.setNodeId(Constants.COMMAND_FAKE_SCENE);
                    dto.setSlot((short) toExec.getId());

                    programToSave = new SoulissCommand(dto);
                } else if (IToSave instanceof SoulissCommand) {
                    programToSave = (SoulissCommand) IToSave;
                    //   programToSave.getCommandDTO().setCommandId(collected.getCommandDTO().getCommandId());
                    programToSave.getCommandDTO().setNodeId((short) Constants.MASSIVE_NODE_ID);
                    programToSave.getCommandDTO().setSlot(((SoulissTypical)outputTypicalSpinner.getSelectedItem()).getTypicalDTO().getTypical());
                }
                //sceneId solo per i comandi che appartengono a una scena
                programToSave.getCommandDTO().setSceneId(null);

                if (programToSave == null) {
                    Toast.makeText(AddProgramActivity.this, "Command not selected", Toast.LENGTH_SHORT);
                    return;
                }
                programToSave.getCommandDTO().setSceneId(null);
                Intent intent = AddProgramActivity.this.getIntent();
                datasource.open();
                if (radioTimed.isChecked()) {// temporal schedule
                    Calendar baseNow = Calendar.getInstance();

                    // se l'ora e` gia passata, fai domani
                    if (commandTimePicker.getCurrentHour().compareTo(baseNow.get(Calendar.HOUR_OF_DAY)) < 0
                            || (commandTimePicker.getCurrentHour().compareTo(baseNow.get(Calendar.HOUR_OF_DAY)) == 0 && commandTimePicker
                            .getCurrentMinute().compareTo(baseNow.get(Calendar.MINUTE)) < 0)) {
                        baseNow.add(Calendar.DAY_OF_YEAR, 1);
                        Log.i(Constants.TAG, "Timed program delayed by one day");
                    }
                    baseNow.set(Calendar.HOUR_OF_DAY, commandTimePicker.getCurrentHour());
                    baseNow.set(Calendar.MINUTE, commandTimePicker.getCurrentMinute());
                    programToSave.getCommandDTO().setType(Constants.COMMAND_TIMED);
                    programToSave.getCommandDTO().setScheduledTime(baseNow);
                    if (checkboxRecursive.isChecked()) {

                        programToSave.getCommandDTO().setInterval(
                                spinnerArrVal[commandSpinnerInterval.getSelectedItemPosition()]);
                    }
                    // inserimento nuovo
                    intent.putExtra("returnedData", Constants.COMMAND_TIMED);
                } else if (radioPositional.isChecked()) {// POSIZIONALE
                    if (togglehomeaway.isChecked()) {
                        programToSave.getCommandDTO().setType(Constants.COMMAND_COMEBACK_CODE);
                        intent.putExtra("returnedData", Constants.COMMAND_COMEBACK_CODE);
                    } else {
                        programToSave.getCommandDTO().setType(Constants.COMMAND_GOAWAY_CODE);
                        intent.putExtra("returnedData", Constants.COMMAND_GOAWAY_CODE);
                    }
                    // inserimento nuovo


                } else if (radioTrigger.isChecked()) {// TRIGGER
                    SoulissTypical trig = ((SoulissTypical) triggeredTypicalSpinner.getSelectedItem());
                    if (trig == null || trig.getTypicalDTO() == null || et.getText() == null
                            || "".compareTo(et.getText().toString()) == 0) {
                        Toast.makeText(AddProgramActivity.this, getString(R.string.programs_warn_trigger_notset),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    intent.putExtra("returnedData", Constants.COMMAND_TRIGGERED);
                    programToSave.getCommandDTO().setType(Constants.COMMAND_TRIGGERED);
                }
                // MERGE
                if (collected != null)
                    programToSave.getCommandDTO().setCommandId(collected.getCommandDTO().getCommandId());
                programToSave.getCommandDTO().persistCommand(datasource);

                if (radioTrigger.isChecked()) {// TRIGGER
                    SoulissTypical trig = ((SoulissTypical) triggeredTypicalSpinner.getSelectedItem());
                    // campi trigger
                    SoulissTriggerDTO trigger = new SoulissTriggerDTO();
                    trigger.setInputNodeId(trig.getTypicalDTO().getNodeId());

                    trigger.setInputSlot(trig.getTypicalDTO().getSlot());
                    trigger.setOp(buttComparator.getText().toString());
                    trigger.setCommandId(programToSave.getCommandDTO().getCommandId());
                    trigger.setThreshVal(Integer.parseInt(et.getText().toString()));

                    trigger.persist(datasource);
                }
                // datasource.close();
                AddProgramActivity.this.setResult(RESULT_OK, intent);

                AddProgramActivity.this.finish();

                return;
            }

        };
        btSave.setOnClickListener(saveProgramButtonListener);
        // Cancel
        OnClickListener simplecan = new OnClickListener() {
            public void onClick(View v) {
                AddProgramActivity.this.finish();
                return;
            }
        };

        btCancel.setOnClickListener(simplecan);
        // start with timed schedule
        radioTimed.performClick();

        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.get("PROG") != null) {
            collected = (SoulissCommand) extras.get("PROG");
            Log.i(Constants.TAG, "Found a SAVED PROGRAM");
            setFields();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Fills a spinner with nodes
     *
     * @param tgt
     */
    private void fillNodeSpinnerWithExtra(Spinner tgt) {
        // spinner popolato in base nodeArray
        ArrayAdapter<SoulissNode> adapter = new ArrayAdapter<SoulissNode>(this, android.R.layout.simple_spinner_item,
                nodiArrayWithExtra);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        tgt.setAdapter(adapter);
    }

    /**
     * Fills a spinner with nodes
     *
     * @param tgt
     */
    private void fillNodeSpinner(Spinner tgt) {
        // spinner popolato in base nodeArray
        ArrayAdapter<SoulissNode> adapter = new ArrayAdapter<SoulissNode>(this, android.R.layout.simple_spinner_item,
                nodiArray);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        tgt.setAdapter(adapter);
    }

    /**
     * popola spinner tipici in base al nodo
     */
    private void setTypicalSpinner(Spinner tgt, SoulissNode ref) {
        if (ref.getId() > Constants.MASSIVE_NODE_ID) {//no fake
            try {
                SoulissTypical[] strArray = new SoulissTypical[ref.getActiveTypicals().size()];
                ref.getActiveTypicals(this).toArray(strArray);

                ArrayAdapter<SoulissTypical> adapter = new ArrayAdapter<SoulissTypical>(this,
                        android.R.layout.simple_spinner_item, strArray);

                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                tgt.setAdapter(adapter);

            } catch (Exception e) {
                Log.e(Constants.TAG, "Errore in setTypicalSpinner:" + e.getMessage(), e);
            }
        } else if (ref.getId() == Constants.COMMAND_FAKE_SCENE) {

            SoulissScene[] strArray = new SoulissScene[scenes.size()];
            strArray = scenes.toArray(strArray);
            ArrayAdapter<SoulissScene> adapter = new ArrayAdapter<SoulissScene>(this,
                    android.R.layout.simple_spinner_item, strArray);

            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            tgt.setAdapter(adapter);
        }
    }

    /**
     * popola spinner comandi in base al tipico. Mette nell'adapter i comandi
     * ottenuti da getCommands
     *
     * @param tgt Spinner da riempire
     * @param ref tipico da cui ottenere i comandi
     */
    private void fillCommandSpinner(Spinner tgt, SoulissTypical ref) {
        tvcommand.setVisibility(View.VISIBLE);
        tgt.setVisibility(View.VISIBLE);
        SoulissCommand[] strArray = new SoulissCommand[ref.getCommands(this).size()];
        ref.getCommands(this).toArray(strArray);

        ArrayAdapter<SoulissCommand> adapter = new ArrayAdapter<SoulissCommand>(this,
                android.R.layout.simple_spinner_item, strArray);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        tgt.setAdapter(adapter);
    }

    /**
     * Mette un comando fake, ovvero la scena che verra\ eseguita
     * NODEID = -2
     *
     * @param tgt Spinner da riempire
     * @param ref tipico da cui ottenere i comandi
     */
    private void fillSceneCommandSpinner(Spinner tgt, SoulissScene ref) {
        tgt.setVisibility(View.INVISIBLE);
        tvcommand.setVisibility(View.INVISIBLE);
        ISoulissExecutable[] strArray = new SoulissScene[1];
        strArray[0] = ref;

        ArrayAdapter<ISoulissExecutable> adapter = new ArrayAdapter<ISoulissExecutable>(this,
                android.R.layout.simple_spinner_item, strArray);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        tgt.setAdapter(adapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // datasource.close();
    }

}
