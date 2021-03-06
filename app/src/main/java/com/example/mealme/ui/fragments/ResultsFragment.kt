package com.example.mealme.ui.fragments

import android.app.AlertDialog
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mealme.model.Meal
import com.example.mealme.ui.adapters.MealAdapter
import android.graphics.Color
import android.view.*
import com.example.mealme.R
import com.example.mealme.net.repositories.RepositoryResult
import com.example.mealme.util.ListOrder
import com.example.mealme.viewmodel.MainViewModel
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.dialog_sort.*
import kotlinx.android.synthetic.main.results_fragment.*
import javax.inject.Inject


class ResultsFragment : DaggerFragment() {

    companion object {
        enum class TYPE {
            FAVOURITES, SEARCH
        }

        fun newInstance(type: TYPE): ResultsFragment {
            val bundle = Bundle().apply { putSerializable("type", type) }
            return ResultsFragment().apply { arguments = bundle }
        }
    }

    private val TAG = this.javaClass.name
    private var fragmentType = TYPE.SEARCH
    private var mealsList = ArrayList<Meal>()
    private lateinit var mealAdapter: MealAdapter

    @Inject lateinit var viewModel: MainViewModel


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {

        fragmentType = arguments?.getSerializable("type") as TYPE

        // Set the observers
        setObservers()

        // Initialize RecyclerView adapter
        mealAdapter = MealAdapter(mealsList, object: MealAdapter.OnItemClickListener {
            override fun OnItemClick(item: Meal) {
                viewModel.selectMeal(item)

                activity?.supportFragmentManager
                    ?.beginTransaction()
                    ?.replace(R.id.container, DetailFragment.newInstance())
                    ?.addToBackStack(null)
                    ?.commit()
            }
        })

        return inflater.inflate(R.layout.results_fragment, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Initially hides everything
        showResults(false)
        // Set type of result
        when(fragmentType) {
            TYPE.FAVOURITES -> {
                fresults.setBackgroundColor(Color.DKGRAY)
                fresults.setPadding(16, 16, 16, 16)
                viewModel.loadFavouritesMeals()
                fresults_title.text = getString(R.string.favourites)
                fresults_error.text = getString(R.string.no_favourites_added)
            }
            TYPE.SEARCH -> {
                viewModel.loadSearchResult()
                fresults_title.text = getString(R.string.search_result)
                fresults_error.text = getString(R.string.no_meals_found)
            }
        }
        // Set buttons handlers
        setButtonsListeners()
        // RecyclerView adapter
        fresults_recyclerview.apply {
            layoutManager = LinearLayoutManager(activity)
            adapter = mealAdapter
        }
    }


    private fun setObservers() {
        viewModel.mealsList.observe(this, Observer { meals ->
            mealsList.clear()
            if (meals.status == RepositoryResult.Status.SUCCESS) {
                if (meals.data!!.isEmpty()) {
                    fresults_error.visibility = View.VISIBLE
                } else {
                    fresults_error.visibility = View.INVISIBLE
                    mealsList.addAll(meals.data)
                }
                sortMealsList()
                showResults(true)
            }
            else if(meals.status == RepositoryResult.Status.ERROR) {
                AlertDialog.Builder(context)
                    .setMessage(meals.message)
                    .show()
                fresults_error.visibility = View.VISIBLE
                showResults(true)
            }
            else {
                showResults(false)
            }
        })
    }


    private fun setButtonsListeners() {
        fresults_sort.setOnClickListener { showSortDialog() }
    }


    private fun showResults(show: Boolean) {
        if(show) {
            fresults_progress.visibility = View.INVISIBLE
            fresults_title.visibility = View.VISIBLE
            fresults_recyclerview.visibility = View.VISIBLE
        } else {
            fresults_progress.visibility = View.VISIBLE
            fresults_title.visibility = View.INVISIBLE
            fresults_error.visibility = View.INVISIBLE
            fresults_recyclerview.visibility = View.INVISIBLE
        }
    }


    private fun showSortDialog() {
        val dialog = AlertDialog.Builder(ContextThemeWrapper(context, R.style.AppTheme))
            .setView(R.layout.dialog_sort)
            .create()

        with(dialog) {
            show()

            // Restore state
            with(viewModel.listOrder) {
                when(field) {
                    ListOrder.FIELD.NAME -> dsort_gf_name.isChecked = true
                    ListOrder.FIELD.CATEGORY -> dsort_gf_category.isChecked = true
                    else -> dsort_gf_shuffle.isChecked = true
                }

                if (field != ListOrder.FIELD.SHUFFLE) {
                    dsort_gd_asc.isEnabled = true
                    dsort_gd_desc.isEnabled = true
                    when (order) {
                        ListOrder.ORDER.ASC -> dsort_gd_asc.isChecked = true
                        ListOrder.ORDER.DESC -> dsort_gd_desc.isChecked = true
                    }
                }
            }

            // Set listeners
            dsort_gf_shuffle.setOnCheckedChangeListener { _, isChecked ->
                with(dsort_gd_asc) {
                    isEnabled = !isChecked
                    setChecked(!isChecked)
                }

                with(dsort_gd_desc) {
                    isEnabled = !isChecked
                    if (isChecked) {
                        setChecked(false)
                    }
                }

                viewModel.listOrder.field = ListOrder.FIELD.SHUFFLE
            }

            dsort_btn_sort.setOnClickListener {
                viewModel.listOrder.field = when(dsort_group_field.checkedRadioButtonId) {
                    R.id.dsort_gf_name -> ListOrder.FIELD.NAME
                    R.id.dsort_gf_category -> ListOrder.FIELD.CATEGORY
                    else -> ListOrder.FIELD.SHUFFLE
                }

                viewModel.listOrder.order = when(dsort_group_direction.checkedRadioButtonId) {
                    R.id.dsort_gd_desc -> ListOrder.ORDER.DESC
                    else -> ListOrder.ORDER.ASC
                }

                sortMealsList()

                dismiss()
            }
        }
    }


    private fun sortMealsList() {
        with(viewModel.listOrder) {
            when(field) {
                ListOrder.FIELD.NAME -> mealsList.sortBy { it.name }
                ListOrder.FIELD.CATEGORY -> mealsList.sortBy { it.category }
                else -> mealsList.shuffle()
            }

            if (order == ListOrder.ORDER.DESC)
                mealsList.reverse()
        }
        mealAdapter.notifyDataSetChanged()
    }
}
