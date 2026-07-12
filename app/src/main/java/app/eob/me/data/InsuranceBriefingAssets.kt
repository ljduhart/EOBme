package app.eob.me.data

import androidx.annotation.DrawableRes
import app.eob.me.R

object InsuranceBriefingAssets {
    @DrawableRes
    fun logoResId(carrier: MajorInsuranceCarrier): Int = when (carrier) {
        MajorInsuranceCarrier.UnitedHealthcare -> R.drawable.briefing_logo_uhc
        MajorInsuranceCarrier.Medicare -> R.drawable.briefing_logo_medicare
        MajorInsuranceCarrier.Aetna -> R.drawable.briefing_logo_aetna
        MajorInsuranceCarrier.BlueCross -> R.drawable.briefing_logo_bcbs
        MajorInsuranceCarrier.Humana -> R.drawable.briefing_logo_humana
    }
}
