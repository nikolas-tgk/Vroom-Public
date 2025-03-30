package com.p17142.vroom.utilities;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.p17142.vroom.R;

public class FragUtils {

    public static void replaceFragment(FragmentManager fragmentManager, Fragment fragment, String transactionName){
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.setCustomAnimations(
                android.R.anim.slide_in_left,  // enter animation for the new fragment
                0 // exit animation for the old fragment
        );
        fragmentTransaction.addToBackStack(transactionName);
        fragmentTransaction.replace(R.id.frameLayout,fragment);
        fragmentTransaction.commit();
    }

}
