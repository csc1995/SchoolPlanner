package schmitt_florian.schoolplanner.activities;

import android.app.DatePickerDialog;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.Calendar;

import schmitt_florian.schoolplanner.R;
import schmitt_florian.schoolplanner.logic.DatabaseHelper;
import schmitt_florian.schoolplanner.logic.DatabaseHelperImpl;
import schmitt_florian.schoolplanner.logic.objects.Homework;
import schmitt_florian.schoolplanner.logic.objects.Subject;

/**
 * bound class to activity_homework_details.xml to show, change attributes of a choose {@link Homework}, delete a choose {@link Homework} or add a new one
 */
public class HomeworkDetailsActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private View rootView;
    private Homework showingHomework;
    private Subject[] subjectsInSpinner;
    private boolean addMode;
    private Button dateButton;
    private DatePickerDialog.OnDateSetListener dateSetListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homework_details);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        dateButton = (Button) findViewById(R.id.homeworkDetails_textDate);
        dateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar cal = Calendar.getInstance();
                int day = cal.get(Calendar.DAY_OF_MONTH);
                int month = cal.get(Calendar.MONTH);
                int year = cal.get(Calendar.YEAR);

                DatePickerDialog dialog = new DatePickerDialog(
                        HomeworkDetailsActivity.this,
                        android.R.style.Theme_Holo_Light_Dialog_MinWidth,
                        dateSetListener,
                        year, month, day);
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.show();
            }
        });

        dateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                month = month + 1;
                String date = day + "." + month + "." + year;
                dateButton.setText(date);
            }
        };


        dbHelper = new DatabaseHelperImpl(this);
        int homeworkID = getIntent().getIntExtra("HomeworkID", -1);
        if (homeworkID <= 0) {
            addMode = true;
        } else {
            addMode = false;
            showingHomework = dbHelper.getHomeworkAtId(homeworkID);
        }

        rootView = findViewById(R.id.homeworkDetails_main);
        initGUI();
    }

    /**
     * saves changes to database
     *
     * @param view the button
     */
    public void onSaveClick(View view) {
        try {
            if (addMode) {
                dbHelper.insertIntoDB(readHomeworkFromGUI());
            } else {
                dbHelper.updateHomeworkAtId(readHomeworkFromGUI());
            }
            finish();
        } catch (IllegalArgumentException ignored) {
        }
    }

    /**
     * deletes homework from database & finishes the activity if deletion successful
     *
     * @param view the button
     */
    public void onDeleteClick(View view) {
        dbHelper.deleteHomeworkAtId(showingHomework.getId());
    }

    /**
     * closes the activity
     *
     * @param view the button
     */
    public void onCloseClick(View view) {
        finish();
    }

    //region private methods

    /**
     * method to initialise components of the GUI
     */
    private void initGUI() {
        if (!addMode) {
            GuiHelper.setTextToTextView(rootView, R.id.homeworkDetails_textDescription, showingHomework.getDescription());
            GuiHelper.setTextToTextView(rootView, R.id.homeworkDetails_textDate,
                    GuiHelper.extractGuiString(showingHomework.getDeadline(), false, rootView.getContext()));
            ((SwitchCompat) findViewById(R.id.homeworkDetails_switchDone)).setChecked(showingHomework.isDone());

            GuiHelper.setVisibility(rootView, R.id.homeworkDetails_buttonDelete, View.VISIBLE);
            GuiHelper.setVisibility(rootView, R.id.homeworkDetails_switchDone, View.VISIBLE);
        } else {
            GuiHelper.setVisibility(rootView, R.id.homeworkDetails_buttonDelete, View.GONE);
            GuiHelper.setVisibility(rootView, R.id.homeworkDetails_switchDone, View.INVISIBLE);
        }

        subjectsInSpinner = fillSpinner();

        //preselect spinner
        if (!addMode) {
            for (int i = 0; i < subjectsInSpinner.length; i++) {
                if (subjectsInSpinner[i].match(showingHomework.getSubject())) {
                    Spinner spinner = (Spinner) findViewById(R.id.homeworkDetails_spinnerSubject);
                    spinner.setSelection(i);
                }
            }
        }
    }

    /**
     * method to fill the Spinner, which shows the {@link Subject}s at the homeworkDetails screen
     *
     * @return returns a array of all {@link Subject}s shown in the spinner ordered by their position in the spinner
     */
    private Subject[] fillSpinner() {
        ArrayList<String> subjectStrings = new ArrayList<>();
        ArrayList<Subject> subjectArrayList = new ArrayList<>();

        int[] subjectIndices = dbHelper.getIndices(DatabaseHelper.TABLE_SUBJECT);

        for (int subjectIndex : subjectIndices) {
            Subject subject = dbHelper.getSubjectAtId(subjectIndex);

            subjectStrings.add(GuiHelper.extractGuiString(subject));
            subjectArrayList.add(subject);
        }

        if (subjectStrings.size() != 0) {
            GuiHelper.fillSpinnerFromArray(rootView, R.id.homeworkDetails_spinnerSubject, subjectStrings.toArray(new String[0]));
        } else {
            GuiHelper.setVisibility(rootView, R.id.homeworkDetails_labelSpinnerError, View.VISIBLE);
            findViewById(R.id.homeworkDetails_buttonSave).setEnabled(false);
        }
        return subjectArrayList.toArray(new Subject[0]);
    }

    /**
     * read the values in the Gui and builds a {@link Homework} from it
     *
     * @return the generated {@link Homework}
     * @throws IllegalArgumentException if input is empty or illegal
     **/
    private Homework readHomeworkFromGUI() throws IllegalArgumentException {
        Spinner spinner = (Spinner) findViewById(R.id.homeworkDetails_spinnerSubject);
        SwitchCompat switchCompat = (SwitchCompat) findViewById(R.id.homeworkDetails_switchDone);

        if (addMode) {
            return new Homework(
                    -1,
                    subjectsInSpinner[spinner.getSelectedItemPosition()],
                    GuiHelper.getInputFromMandatoryEditText(rootView, R.id.homeworkDetails_textDescription),
                    GuiHelper.getDateFromMandatoryButton(rootView, R.id.homeworkDetails_textDate),
                    false
            );
        } else {
            return new Homework(
                    showingHomework.getId(),
                    subjectsInSpinner[spinner.getSelectedItemPosition()],
                    GuiHelper.getInputFromMandatoryEditText(rootView, R.id.homeworkDetails_textDescription),
                    GuiHelper.getDateFromMandatoryButton(rootView, R.id.homeworkDetails_textDate),
                    switchCompat.isChecked()
            );
        }

    }
    //endregion
}
