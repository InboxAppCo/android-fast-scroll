package co.inbox.ui.fast_scroll;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import co.inbox.ui.inboxfastscroll.FastScrollAdapter;
import co.inbox.ui.inboxfastscroll.InboxFastScrollView;

public class TestActivity
        extends AppCompatActivity {
    private static final String TAG = "TestActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        RecyclerView recycler = (RecyclerView) findViewById(R.id.recycler);
        InboxFastScrollView scroller = (InboxFastScrollView) findViewById(R.id.fast_scroller);

        Adapter adapter = new Adapter();
        recycler.setAdapter(adapter);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        scroller.setRecyclerView(recycler);
        scroller.setFastScrollAdapter(adapter);
    }

    ///////////////////////////////////////////////////////////////////////////////
    // Adapter
    ///////////////////////////////////////////////////////////////////////////////

    static class ViewHolder
            extends RecyclerView.ViewHolder {
        TextView content;

        public ViewHolder(View itemView) {
            super(itemView);
            content = (TextView) itemView.findViewById(R.id.content);
        }
    }

    private static class Adapter
            extends RecyclerView.Adapter<ViewHolder>
            implements FastScrollAdapter {
        private static final int ITEM_COUNT = 20;

        List<String> mStrings;

        {
            mStrings = new ArrayList<>();
            Random r = new java.util.Random();
            for (int index = 0; index < ITEM_COUNT; index++) {
                mStrings.add(((char) ('A' + r.nextInt('Z' - 'A'))) + " " + Integer.toString(index));
            }
            Collections.sort(mStrings);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int position) {
            View view = LayoutInflater.from(parent.getContext())
                                      .inflate(R.layout.list_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, int position) {
            viewHolder.content.setText(mStrings.get(position));
        }

        @Override
        public int getItemCount() {
            return ITEM_COUNT;
        }

        @Override
        public String getTextForPosition(int position) {
            return mStrings.get(position).substring(0, 1);
        }
    }
}
