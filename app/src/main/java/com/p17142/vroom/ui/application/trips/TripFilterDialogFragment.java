package com.p17142.vroom.ui.application.trips;

import static com.p17142.vroom.utilities.Constants.IS_SUCCESSFUL;
import static com.p17142.vroom.utilities.Constants.KEY_ANYWHERE;
import static com.p17142.vroom.utilities.Constants.KEY_FILTER_END_DATE;
import static com.p17142.vroom.utilities.Constants.KEY_FILTER_SHOW_FULL;
import static com.p17142.vroom.utilities.Constants.KEY_FILTER_SHOW_OWNED;
import static com.p17142.vroom.utilities.Constants.KEY_FILTER_START_DATE;
import static com.p17142.vroom.utilities.Constants.KEY_FILTER_TRIP_END;
import static com.p17142.vroom.utilities.Constants.KEY_FILTER_TRIP_START;
import static com.p17142.vroom.utilities.Constants.KEY_FUTURE;
import static com.p17142.vroom.utilities.Constants.REQUEST_KEY_FILTER_UPDATE;
import static com.p17142.vroom.utilities.Constants.REQUEST_KEY_RATE_SUBMIT;
import static com.p17142.vroom.utilities.Constants.REQUEST_KEY_RATING_COMPLETE;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.fragment.app.DialogFragment;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.p17142.vroom.R;
import com.p17142.vroom.databinding.FragmentTripFilterDialogBinding;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.view.inputmethod.InputMethodManager;

public class TripFilterDialogFragment extends DialogFragment {

    private FragmentTripFilterDialogBinding binding;

    private String[] cities;
    private Date startDate = null ;
    private Date endDate = null ;

    private long startDateL = -1;
    private long endDateL = -1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setStyle(STYLE_NORMAL, R.style.AppTheme_DialogOverlay);
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentTripFilterDialogBinding.inflate( inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        init();
        setListeners();
    }

    private void init(){

        binding.tripStartLayout.setHelperTextEnabled(false);
        binding.tripEndLayout.setHelperTextEnabled(false);
        binding.dateLayout.setHelperTextEnabled(false);

        binding.tripStartAutoCompleteTextView.setText("");
        binding.tripEndAutoCompleteTextView.setText("");
        binding.dateInputText.setText("");


        cities = getResources().getStringArray(R.array.cities_eng);

        ArrayAdapter<String> citiesFromAdapter = new ArrayAdapter<>(requireContext(),R.layout.item_dropdown_cities,cities);
        binding.tripStartAutoCompleteTextView.setAdapter(citiesFromAdapter);

        // adapter needs to be different or they will share the same autocomplete results
        ArrayAdapter<String> citiesToAdapter = new ArrayAdapter<>(requireContext(),R.layout.item_dropdown_cities,cities);
        binding.tripEndAutoCompleteTextView.setAdapter(citiesToAdapter);

        if(binding.tripStartAutoCompleteTextView.getText().toString().isEmpty())
        {
            binding.deleteFromCityImage.setVisibility(View.INVISIBLE);
        }

        if(binding.tripEndAutoCompleteTextView.getText().toString().isEmpty())
        {
            binding.deleteEndCityImage.setVisibility(View.INVISIBLE);
        }
        tryChangeSwapVisibility();

        if(binding.dateInputText.getText().toString().isEmpty()) // default hardcoded value here
        {
            binding.deleteDateRangeImage.setVisibility(View.INVISIBLE);
        }
    }

    private void setListeners(){

        binding.closeImage.setOnClickListener( unused -> dismiss() );

        binding.searchButton.setOnClickListener( unused -> onSearchClick());

        binding.deleteFromCityImage.setOnClickListener( unused -> clearFromCityClick());

        binding.deleteEndCityImage.setOnClickListener( unused -> clearEndCityClick());

        binding.switchCitiesImage.setOnClickListener( unused -> onSwapCitiesClick());

        binding.resetButton.setOnClickListener( unused -> onResetButtonClick());

        binding.dateInputText.setOnClickListener( unused -> onDateClick());

        binding.deleteDateRangeImage.setOnClickListener( unused -> clearDatePicker());


        binding.tripStartAutoCompleteTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if(charSequence.length() == 0)
                {
                    binding.deleteFromCityImage.setVisibility(View.INVISIBLE);
                    tryChangeSwapVisibility();
                }
                else{
                    if(binding.deleteFromCityImage.getVisibility() == View.INVISIBLE)
                    {
                        binding.deleteFromCityImage.setVisibility(View.VISIBLE);
                        tryChangeSwapVisibility();
                    }
                }
            }
            @Override
            public void afterTextChanged(Editable editable) {
                tryDisableFromError();
            }
        });

        binding.tripEndAutoCompleteTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if(charSequence.length() == 0)
                {
                    binding.deleteEndCityImage.setVisibility(View.INVISIBLE);
                    tryChangeSwapVisibility();
                }
                else{
                    if(binding.deleteEndCityImage.getVisibility() == View.INVISIBLE)
                    {
                        binding.deleteEndCityImage.setVisibility(View.VISIBLE);
                        tryChangeSwapVisibility();
                    }
                }
            }
            @Override
            public void afterTextChanged(Editable editable) {
                tryDisableEndError();
            }
        });

        binding.dateInputText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if(charSequence.length() == 0){
                    binding.deleteDateRangeImage.setVisibility(View.INVISIBLE);
                }
                else{
                    binding.deleteDateRangeImage.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    private boolean isInputValid(){
        String fromCityText = binding.tripStartAutoCompleteTextView.getText().toString().trim();
        String endCityText = binding.tripEndAutoCompleteTextView.getText().toString().trim();

        boolean fromCityValid = false;
        boolean endCityValid = false;


        if(fromCityText.isEmpty())
        {
            fromCityValid = true;
        }
        if(endCityText.isEmpty())
        {
            endCityValid = true;
        }

        for (String city: cities
        ) {
            if(city.equals(fromCityText))
            {
                fromCityValid = true;
            }
            if(city.equals(endCityText))
            {
                endCityValid = true;
            }
        }

        if(!fromCityValid && !endCityValid){
            binding.tripStartLayout.setError("Invalid city");
            binding.tripStartLayout.setErrorEnabled(true);

            binding.tripEndLayout.setError("Invalid city");
            binding.tripEndLayout.setErrorEnabled(true);
            return false;
        }
        else if(!fromCityValid){
            binding.tripStartLayout.setError("Invalid city");
            binding.tripStartLayout.setErrorEnabled(true);
            return false;
        } else if(!endCityValid)
        {
            binding.tripEndLayout.setError("Invalid city");
            binding.tripEndLayout.setErrorEnabled(true);
            return false;
        }
        if(fromCityText.equals(endCityText) && !fromCityText.isEmpty())
        {
            binding.tripEndLayout.setError("You must select a different city");
            binding.tripEndLayout.setErrorEnabled(true);
            return false;
        }
        return true;
    }

    // returns millis value of now, used in datepicker
    private long getTodayInMillis(){
        Calendar calendar = Calendar.getInstance();
        return calendar.getTimeInMillis();
    }

    private void onDateClick(){
        binding.dateInputText.setEnabled(false); // disable so to not multi click/call OnClickListeners, will re-enable on finish/cancel
        binding.dateLayout.setEnabled(false);
        binding.loadingBar.setVisibility(View.VISIBLE);
        CalendarConstraints.Builder calendarConstraintsBuilder = new CalendarConstraints.Builder();
        long todayInMillis = getTodayInMillis();

        calendarConstraintsBuilder.setStart(todayInMillis); // setting up "no previous dates" constraint to apply later on datePicker builder.
        CalendarConstraints.DateValidator dateValidator = DateValidatorPointForward.from(todayInMillis); // this excludes today
        calendarConstraintsBuilder.setValidator(dateValidator);

        MaterialDatePicker<Pair<Long, Long>> datePicker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Select Date")
                .setCalendarConstraints(calendarConstraintsBuilder.build()) // apply constraint (no previous dates)
                .build();

        datePicker.addOnPositiveButtonClickListener( selection -> {

            Date selectedDateStart = new Date(selection.first);
            Date selectedDateEnd = new Date(selection.second);
            startDate = selectedDateStart;
            endDate = selectedDateEnd;

            startDateL = selection.first;
            endDateL = selection.second;

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM"); // format date to e.g. 14 Jan
            binding.dateInputText.setText(dateFormat.format(selectedDateStart)+" - "+dateFormat.format(selectedDateEnd)); // print formated date

            binding.dateInputText.setEnabled(true); // re-enable
            binding.dateLayout.setEnabled(true);
            binding.loadingBar.setVisibility(View.INVISIBLE);

        });
        datePicker.show(requireActivity().getSupportFragmentManager(), "tag");

        datePicker.addOnCancelListener( unused2 -> {
            binding.dateInputText.setEnabled(true);
            binding.dateLayout.setEnabled(true);
            binding.loadingBar.setVisibility(View.INVISIBLE);
        }); // re-enable datePicker on cancel
        datePicker.addOnDismissListener( unused2 -> {
            binding.dateInputText.setEnabled(true);
            binding.dateLayout.setEnabled(true);
            binding.loadingBar.setVisibility(View.INVISIBLE);
        }); // on dismiss
    }

    private void onResetButtonClick(){
        clearFromCityClick();
        clearEndCityClick();
        clearDatePicker();
        binding.fullTripsCheckBox.setChecked(true);
        binding.ownTripsCheckBox.setChecked(true);
    }

    private void tryChangeSwapVisibility()
    {
        if(binding.deleteEndCityImage.getVisibility() == View.VISIBLE || binding.deleteFromCityImage.getVisibility() == View.VISIBLE)
        {
            binding.switchCitiesImage.setVisibility(View.VISIBLE);
        } else if (binding.switchCitiesImage.getVisibility() == View.VISIBLE) {
            binding.switchCitiesImage.setVisibility(View.INVISIBLE);

        }
    }

    private void tryDisableFromError(){
        if(binding.tripStartLayout.isErrorEnabled())
        {
            binding.tripStartLayout.setErrorEnabled(false);
        }
    }
    private void tryDisableEndError(){
        if(binding.tripEndLayout.isErrorEnabled())
        {
            binding.tripEndLayout.setErrorEnabled(false);
        }
    }

    private void clearFromCityClick(){
        binding.tripStartAutoCompleteTextView.setText("");
        binding.tripStartLayout.setErrorEnabled(false);
        // binding.deleteFromCityImage.setVisibility(View.INVISIBLE);
    }

    private void clearEndCityClick(){
        binding.tripEndAutoCompleteTextView.setText("");
        binding.tripEndLayout.setErrorEnabled(false);
        //binding.deleteEndCityImage.setVisibility(View.INVISIBLE);

    }

    private void clearDatePicker(){
        binding.dateInputText.setText("");
        startDate = null;
        endDate = null;

        startDateL = -1;
        endDateL = 1;
        //binding.deleteDateRangeImage.setVisibility(View.INVISIBLE);
    }

    private void onSwapCitiesClick(){
        String startCityText = binding.tripStartAutoCompleteTextView.getText().toString();
        String endCityText = binding.tripEndAutoCompleteTextView.getText().toString();
        binding.tripStartAutoCompleteTextView.setText(endCityText);
        binding.tripEndAutoCompleteTextView.setText(startCityText);

        binding.tripEndAutoCompleteTextView.clearFocus();
        binding.tripStartAutoCompleteTextView.clearFocus();

        closeKeyboard();

    }
    private void onSearchClick(){
        if(isInputValid()){

            Bundle result = new Bundle();

            result.putBoolean(KEY_FILTER_SHOW_OWNED, !binding.ownTripsCheckBox.isChecked() );
            result.putBoolean(KEY_FILTER_SHOW_FULL, !binding.fullTripsCheckBox.isChecked() );

            if(startDate == null || endDate == null)
            {
                result.putLong(KEY_FILTER_START_DATE,-1); // an invalid time value
                result.putLong(KEY_FILTER_END_DATE,-1);
            }
            else{
                result.putLong(KEY_FILTER_START_DATE, startDateL);
                result.putLong(KEY_FILTER_END_DATE, endDateL);
            }

            String tripStartCity = binding.tripStartAutoCompleteTextView.getText().toString().trim(); // From-To Locations already validated
            if(!tripStartCity.isEmpty())
            {
                result.putString(KEY_FILTER_TRIP_START,tripStartCity);
            }
            else {
                result.putString(KEY_FILTER_TRIP_START,KEY_ANYWHERE);
            }

            String tripEndCity = binding.tripEndAutoCompleteTextView.getText().toString().trim();
            if(!tripEndCity.isEmpty())
            {
                result.putString(KEY_FILTER_TRIP_END,tripEndCity);
            }
            else{
                result.putString(KEY_FILTER_TRIP_END,KEY_ANYWHERE);
            }

            getParentFragmentManager().setFragmentResult(REQUEST_KEY_FILTER_UPDATE, result);
            dismiss();
        }
    }

    private void closeKeyboard(){
        // code to close keyboard, this is fragment specific
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        View view = requireView().getRootView(); // get root-view
        if (imm != null && view != null
        ) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        onDialogDismissed();
    }

    private void onDialogDismissed() {
        if (getParentFragment() instanceof OnDialogDismissListener) {
            ((OnDialogDismissListener) getParentFragment()).onDialogDismissed(); //notify parent
        }
    }
    // callback listener
    public interface OnDialogDismissListener {
        void onDialogDismissed();
    }
}