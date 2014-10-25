package com.gdg.wearbong;

import android.app.Fragment;
import android.content.Context;
import android.app.FragmentManager;
import android.support.wearable.view.CardFragment;
import android.support.wearable.view.FragmentGridPagerAdapter;

/**
 * Created by youngbin on 14. 10. 25.
 */
public class WearGridPagerAdapter extends FragmentGridPagerAdapter {

    private Context mContext;

    public WearGridPagerAdapter(Context ctx,FragmentManager fm) {
        super(fm);
        mContext = ctx;
    }

    @Override
    public int getRowCount() {
        return Pages.length;
    }

    // Obtain the number of pages (horizontal)
    @Override
    public int getColumnCount(int rowNum) {
        return Pages[rowNum].length;
    }


    /** A simple container for static data in each page */
    private static class Page {
//        int cardGravity = Gravity.BOTTOM;
        Class Frag;

        public Page(Class frag/*, int gravity*/) {
          //  this.cardGravity = gravity;
            this.Frag = frag;
        }
    }

    private final Page[][] Pages = {
            {new Page(BlankFragment.class),new Page(BlankFragment.class)},
            {new Page(BlankFragment.class),new Page(BlankFragment.class)}
    };
        // Override methods in FragmentGridPagerAdapter

    private Fragment imageFragment;

    @Override
    public Fragment getFragment(int row, int col) {
        Page page = Pages[row][col];
        Fragment fragment = Fragment.instantiate(mContext, page.Frag.getName());


//        CardFragment card = CardFragment.create
        // Advanced settings
//        fragment.setCardGravity(page.cardGravity);
//        fragment.setExpansionEnabled(page.expansionEnabled);
//        fragment.setExpansionDirection(page.expansionDirection);
//        fragment.setExpansionFactor(page.expansionFactor);
        return fragment;
    }
        }